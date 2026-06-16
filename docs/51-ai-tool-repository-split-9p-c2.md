# 51. AI Tool Repository Split — 9P-C.2

## Objetivo

Separar la capa read-only del asistente IA para que los servicios por dominio deleguen el acceso a datos en repositories por dominio.

Esta fase mantiene el comportamiento externo del asistente y no agrega fallback Tool/RAG ni LLM-driven planning.

## Cambios principales

- `AiReadOnlyToolService` se mantiene como fachada pública usada por handlers existentes.
- Los domain services bajo `com.tamias.ai.tool.service` ya no extienden `AiReadOnlyToolSupport`.
- Cada domain service ahora delega en un repository específico bajo `com.tamias.ai.tool.repository`.
- `AiReadOnlyToolSupport` queda como base compartida de helpers SQL, normalización, formatting y utilidades legacy.

## Nuevos repositories

- `AssistantProfileToolRepository`
- `UserRoleOrganizationToolRepository`
- `AiChatHistoryToolRepository`
- `PropertyCatalogToolRepository`
- `ScheduledReservationGuestToolRepository`
- `MaintenanceToolRepository`
- `PurchaseToolRepository`
- `ReservationSupplyTaskToolRepository`
- `DocumentRagToolRepository`
- `InventoryToolRepository`
- `FileImageToolRepository`
- `DashboardToolRepository`

## Diseño actual

```text
Handler
  -> AiReadOnlyToolService facade
      -> DomainReadOnlyToolService
          -> DomainToolRepository
              -> AiReadOnlyToolSupport shared helpers
```

## Por qué se hizo así

La fase 9P-C.2 evita un cambio masivo de comportamiento. Todavía existen helpers compartidos en `AiReadOnlyToolSupport`, pero el acceso por dominio ya tiene un boundary repository explícito. Esto prepara el terreno para que 9P-D pueda convertir respuestas de tools en estados como `HIT`, `EMPTY`, `DENIED` y `GUARDRAIL`.

## Qué no cambia

- No cambia el orden de routing.
- No cambia la firma pública de `AiReadOnlyToolService`.
- No cambia la estructura de handlers.
- No cambia SQL ni respuestas esperadas.
- No agrega Tool/RAG fallback.
- No agrega LLM-driven planning.

## Siguiente fase sugerida

`9P-D — Tool/RAG fallback real`

Ahí se debe empezar a usar `AiToolResultStatus` para que una tool pueda indicar si realmente encontró datos (`HIT`) o si no encontró nada (`EMPTY`) y permitir fallback seguro a RAG cuando aplique.
