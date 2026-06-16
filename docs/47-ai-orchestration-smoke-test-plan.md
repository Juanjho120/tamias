# 47 — AI Orchestration Smoke Test Plan

Use this checklist after 9P-A to confirm the refactor did not change external behavior.

## General assistant

```text
¿Qué puedes hacer?
Crea una reservación para mañana
¿Cómo me llamo?
¿Cuál es mi correo?
¿Cuál es mi rol?
¿Cuál es mi organización?
```

## Property and catalog

```text
¿Qué propiedades tengo?
Dame un resumen de Bungalow Tu Refugio Perfecto
¿Qué propiedades activas tengo?
¿Qué propiedades inactivas tengo?
¿Qué catálogos puedo usar para mantenimiento?
¿Qué plataformas de reservación tengo?
¿Qué tipos de inventory item existen?
```

## Inventory and maintenance

```text
¿Qué items se usan más?
¿Qué supplies se usan más en reservaciones?
¿Dónde se ha usado el café?
¿Qué items se usaron en mantenimientos?
¿Qué propiedad tiene más gastos de mantenimiento?
¿Cuánto gasté en mantenimiento por categoría?
```

## Scheduled maintenance, reservations and guests

```text
¿Qué mantenimientos programados están vencidos?
¿Qué mantenimiento programado vence hoy?
¿Cuál es el próximo mantenimiento programado?
¿Cuál es la próxima entrada?
¿Cuál es la próxima salida?
¿Qué reservaciones tengo esta semana?
¿Qué huéspedes regresan?
```

## Reservation supplies and tasks

```text
¿Qué supplies necesito para la próxima reserva?
¿Qué supplies se usaron en la última reserva?
¿Qué supplies se usan más?
¿Qué reservaciones próximas no tienen supplies asignados?
¿Qué tareas tengo pendientes?
¿Qué tareas hay para la próxima reservación?
¿Qué tareas ya se completaron?
¿Qué tareas están asignadas por persona?
```

## Purchases

```text
¿Cuándo compré por última vez cloro?
¿Cuánto cuesta normalmente el papel higiénico?
¿Qué item compro más seguido?
¿Qué item compro menos seguido?
¿Cuánto gasté en compras por mes?
¿Cuánto gasté en compras por propiedad?
```

## Documents and RAG metadata

```text
¿Qué documentos tengo cargados?
¿Qué documentos tengo por tipo?
¿Qué documentos tengo por propiedad?
¿Qué documentos están procesados?
¿Qué documentos están procesados pero no indexados para IA?
¿Qué documentos fallaron al procesarse?
¿Cómo está el índice RAG de mis documentos?
¿Qué documentos están listos para IA?
```

## Files, images and dashboard

```text
¿Qué archivos asociados a documentos tengo?
¿Qué archivos asociados a mantenimientos tengo?
¿Qué mantenimientos tienen imágenes?
¿Qué alertas operativas tengo?
¿Qué necesita atención hoy?
Dame un resumen de compras del dashboard
Dame un resumen de tareas del dashboard
```

## Admin-only user, role and organization

Run these as an administrator:

```text
¿Qué usuarios activos tengo?
¿Qué usuarios inactivos tengo?
¿Qué usuarios tienen rol Maintenance Staff?
¿Qué accesos tengo?
¿Qué accesos tiene este usuario?
Dame el resumen de accesos de todos los usuarios.
¿Qué roles existen?
¿Qué permisos tiene Maintenance Staff?
¿Qué permisos tiene Administrator?
¿Cuántos usuarios tiene mi organización?
¿Qué módulos estamos usando más?
```

Run these as a non-admin user and confirm they are denied safely:

```text
¿Qué usuarios activos tengo?
¿Qué permisos tiene Administrator?
¿Cuántos usuarios tiene mi organización?
```

## AI chat history

```text
Muéstrame mis últimas conversaciones con la IA
¿Qué hemos hablado antes?
Busca en el historial si hablamos de cloro
¿Qué preguntas le hice al asistente?
Resume esta conversación
¿Cuántas sesiones de chat IA tengo?
¿Qué chats tengo sobre Bungalow Tu Refugio Perfecto?
```

## RAG fallback scenarios to validate manually before 9P-B

These may still expose the current limitation. They are documented here to guide the next phase:

```text
Pregunta sobre una regla que solo existe dentro de un documento RAG.
Pregunta algo que una tool detecta parcialmente pero no encuentra en datos estructurados.
Pregunta algo ambiguo que podría requerir tools + RAG.
```

Expected for 9P-A: behavior should remain the same as before the refactor.

Expected for 9P-B: the assistant should support Tool EMPTY -> RAG fallback and a final combined not-found message.
