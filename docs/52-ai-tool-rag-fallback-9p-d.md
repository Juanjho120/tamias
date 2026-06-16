# 9P-D — Tool/RAG fallback real

## Objetivo

Esta fase estabiliza el flujo del asistente IA para que una tool read-only que sí aplica a la pregunta, pero no encuentra datos, no bloquee automáticamente la búsqueda documental RAG.

Antes de esta fase, el flujo era rígido:

```text
Tool match -> respuesta inmediata
No tool match -> RAG
```

Eso provocaba que una pregunta documental pudiera quedar atrapada por una tool de metadata o de sistema y responder solo “no encontré datos”, sin intentar buscar dentro de los documentos indexados.

## Nuevo flujo

El flujo ahora distingue el resultado de una tool:

```text
HIT            -> responder con datos de la tool
EMPTY          -> si la política lo permite, buscar en RAG
DENIED         -> responder sin fallback por seguridad
GUARDRAIL      -> responder sin fallback por seguridad
ERROR          -> preparado para fallback controlado
NOT_APPLICABLE -> buscar en RAG como antes
```

## Contratos usados

Se mantiene `AiToolAnswer` para compatibilidad con handlers y servicios existentes.

La clasificación real se hace con:

- `AiToolResult`
- `AiToolResultStatus`
- `AiToolFallbackPolicy`

`AiToolCallingService` sigue ejecutando handlers ordenados, pero ahora clasifica la respuesta en lugar de tratar cualquier `AiToolAnswer` como éxito definitivo.

## Política de fallback

### No hacen fallback a RAG

- `DENIED`: consultas admin-only sin permiso.
- `GUARDRAIL`: intentos de escritura o acciones no permitidas.
- Tools de usuarios, roles y organización.
- Tools de historial IA.
- Dashboard y métricas operativas estrictas.
- RAG health metadata (`rag.*`).

### Sí pueden hacer fallback a RAG si están vacías

- Document metadata (`document.*`).
- File/image metadata (`file.*`, `image.*`).
- Property/catalog/maintenance/reservation/purchase/inventory/task cuando la pregunta pueda tener explicación documental.
- Assistant-level composed tools cuando no haya evidencia suficiente y la pregunta pueda apoyarse en documentos.

## AiRagService

`AiRagService` ahora usa `toolCallingService.tryHandleResult(request)`.

Si una tool devuelve `HIT`, responde como antes.

Si devuelve `EMPTY` con `allowRagFallback=true`, busca documentos similares en Chroma/RAG.

Si RAG encuentra fuentes, usa el LLM con contexto documental y adjunta también la evidencia de la tool vacía.

Si RAG tampoco encuentra fuentes, responde con un mensaje final unificado indicando que revisó datos del sistema y documentos indexados.

## Mensaje final cuando todo falla

Cuando una tool aplicó pero no encontró registros y RAG tampoco encontró documentos relacionados, el usuario recibe un mensaje similar a:

```text
No encontré información relacionada con lo que preguntaste.

Revisé:
- Datos del sistema: la tool aplicable no encontró registros suficientes.
- Documentos indexados/RAG: no encontré contenido relacionado.
```

Esto evita respuestas incompletas donde el asistente parece haberse cerrado únicamente en una tool.

## Límites de esta fase

Esta fase todavía no implementa LLM-driven planning.

El orden de handlers sigue siendo determinístico y controlado por backend.

El LLM participa cuando el flujo llega a RAG y hay contexto documental disponible.

La siguiente fase puede usar estos estados para permitir planificación más inteligente con LLM:

- tool only
- RAG only
- tool + RAG
- ask clarification
- deny action

## Pruebas sugeridas

### Tool HIT: no debe ir a RAG

```text
¿Qué usuarios activos tengo?
¿Qué permisos tiene Maintenance Staff?
¿Qué alertas operativas tengo?
```

### Tool EMPTY con fallback a RAG

Usar una pregunta que antes caía en metadata pero realmente era documental:

```text
¿Qué dice el documento sobre visitantes?
¿Qué reglas hay sobre basura?
¿Qué menciona el manual sobre filtros?
```

Si la tool no encuentra metadata suficiente pero los documentos indexados sí tienen contenido, debe responder desde RAG con fuentes `[S1]`, `[S2]`.

### Tool EMPTY + RAG EMPTY

```text
¿Qué dice el documento sobre una regla que no existe?
```

Debe responder con el mensaje final unificado.

### Seguridad sin fallback

```text
Crea una reservación para mañana.
¿Qué usuarios activos tengo?  // usando usuario no admin
```

Debe responder con guardrail o denied, sin buscar en RAG.
