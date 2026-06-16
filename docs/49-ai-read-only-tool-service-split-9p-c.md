# 9P-C — Split AiReadOnlyToolService into domain services

## Goal

Split the large `AiReadOnlyToolService` facade into domain-oriented read-only tool services without changing external behavior.

## Scope

This phase keeps the existing handler contracts intact. Handlers still inject and call `AiReadOnlyToolService`, but that class is now a facade that delegates to domain services.

## New structure

```text
ai/tool/
  AiReadOnlyToolService.java                    # facade used by existing handlers
  AiReadOnlyToolSupport.java                    # shared legacy support during transition
  AssistantProfileReadOnlyToolService.java
  UserRoleOrganizationReadOnlyToolService.java
  AiChatHistoryReadOnlyToolService.java
  PropertyCatalogReadOnlyToolService.java
  ScheduledReservationGuestReadOnlyToolService.java
  MaintenanceReadOnlyToolService.java
  PurchaseReadOnlyToolService.java
  ReservationSupplyTaskReadOnlyToolService.java
  DocumentRagReadOnlyToolService.java
  InventoryReadOnlyToolService.java
  FileImageReadOnlyToolService.java
  DashboardReadOnlyToolService.java
```

## Behavior

No external behavior is intentionally changed in this phase. The public method names used by handlers are preserved in `AiReadOnlyToolService`.

## Module boundaries

- Assistant/profile: assistant capabilities, current user profile and current organization summary.
- Admin: users, roles and organization admin counters.
- Chat history: `aiChat.*` history tools.
- Property/catalog: properties and catalog-style lookups.
- Scheduled/reservation/guest: reservations, guests and scheduled maintenance.
- Maintenance: maintenance records, costs and image summaries.
- Purchase: purchase lists and purchase items.
- Reservation supply/task: reservation supplies, task lists and task items.
- Document/RAG: document metadata and RAG health.
- Inventory: inventory analytics.
- File/image: file metadata and image metadata.
- Dashboard: dashboard summaries, alerts and operational summary.

## Why keep a facade?

The facade reduces migration risk. Existing handlers do not need to change in this phase. The next refactor can move handlers to depend directly on the domain services.

## Important limitation

`AiReadOnlyToolSupport` still contains shared legacy logic so this phase is a safe intermediate split rather than the final repository/service extraction. Future work should continue moving SQL and formatting logic into narrower repositories or query services.

## Next recommended phase

`9P-D — Tool/RAG fallback with AiToolResultStatus`

After the service split, the orchestrator can start distinguishing `HIT`, `EMPTY`, `DENIED`, `GUARDRAIL`, and `NOT_APPLICABLE` results.
