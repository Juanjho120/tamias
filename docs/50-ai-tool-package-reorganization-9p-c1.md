# 50 — AI tool package reorganization (9P-C.1)

## Objective

Reorganize the AI tool code into clearer Java packages without changing external behavior.

This phase is intentionally structural only. It does not introduce Tool/RAG fallback, LLM-driven planning, repository splitting, or response behavior changes.

## Package layout

```text
com.tamias.ai.tool
  AiToolAnswer
  AiToolCallingService
  AiToolResult
  AiToolResultStatus

com.tamias.ai.tool.context
  AiToolRequestContext

com.tamias.ai.tool.handler
  AiToolHandler
  AssistantCoreToolHandler
  UserRoleOrganizationToolHandler
  CurrentOrganizationToolHandler
  AiChatHistoryToolHandler
  FileImageDashboardToolHandler
  PriorityMaintenanceAnalyticsToolHandler
  ReservationSupplyTaskToolHandler
  PurchaseAnalyticsToolHandler
  DocumentRagMetadataToolHandler
  InventoryToolHandler
  ScheduledReservationGuestToolHandler
  AssistantLevelToolHandler
  PropertyToolHandler
  CatalogToolHandler
  MaintenanceAnalyticsToolHandler
  LegacyFallbackToolHandler

com.tamias.ai.tool.service
  AiReadOnlyToolService
  AssistantProfileReadOnlyToolService
  UserRoleOrganizationReadOnlyToolService
  AiChatHistoryReadOnlyToolService
  PropertyCatalogReadOnlyToolService
  ScheduledReservationGuestReadOnlyToolService
  MaintenanceReadOnlyToolService
  PurchaseReadOnlyToolService
  ReservationSupplyTaskReadOnlyToolService
  DocumentRagReadOnlyToolService
  InventoryReadOnlyToolService
  FileImageReadOnlyToolService
  DashboardReadOnlyToolService

com.tamias.ai.tool.support
  AiReadOnlyToolSupport
  AiToolRoutingSupport
  AiToolTextNormalizer
```

## Behavior

No functional behavior is intended to change. The Spring component scan still discovers all handlers and services under `com.tamias.ai`.

The handler execution order is still controlled by `@Order` annotations.

## Important migration note

Because ZIP extraction cannot delete files, remove the old Java files that were previously under:

```text
backend/src/main/java/com/tamias/ai/tool/*ToolHandler.java
backend/src/main/java/com/tamias/ai/tool/*ReadOnlyToolService.java
backend/src/main/java/com/tamias/ai/tool/AiToolHandler.java
backend/src/main/java/com/tamias/ai/tool/AiToolRequestContext.java
backend/src/main/java/com/tamias/ai/tool/AiToolRoutingSupport.java
backend/src/main/java/com/tamias/ai/tool/AiReadOnlyToolSupport.java
backend/src/main/java/com/tamias/ai/tool/AiToolTextNormalizer.java
```

Keep these files in `backend/src/main/java/com/tamias/ai/tool/`:

```text
AiToolAnswer.java
AiToolCallingService.java
AiToolResult.java
AiToolResultStatus.java
```

## Next phase

9P-C.2 should split SQL/query logic into repositories by domain.
