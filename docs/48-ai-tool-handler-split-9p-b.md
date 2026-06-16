# 9P-B — AI tool handler split without external behavior changes

## Goal

Split the AI assistant tool routing into module-oriented handlers while keeping the external behavior unchanged.

This phase continues the stabilization work started in 9P-A. It does not introduce LLM-driven planning, Tool/RAG fallback changes, or new tools. The goal is to make the routing layer maintainable before changing orchestration behavior.

## What changed

`AiToolCallingService` is now a small orchestrator. It builds an `AiToolRequestContext` and evaluates ordered `AiToolHandler` beans until one returns an answer.

The previous routing order was preserved using Spring `@Order` annotations.

## New routing structure

```text
AiToolCallingService
  -> AssistantCoreToolHandler
  -> UserRoleOrganizationToolHandler
  -> CurrentOrganizationToolHandler
  -> AiChatHistoryToolHandler
  -> FileImageDashboardToolHandler
  -> PriorityMaintenanceAnalyticsToolHandler
  -> ReservationSupplyTaskToolHandler
  -> PurchaseAnalyticsToolHandler
  -> DocumentRagMetadataToolHandler
  -> InventoryToolHandler
  -> ScheduledReservationGuestToolHandler
  -> AssistantLevelToolHandler
  -> PropertyToolHandler
  -> CatalogToolHandler
  -> MaintenanceAnalyticsToolHandler
  -> LegacyFallbackToolHandler
```

## New support classes

```text
AiToolHandler
AiToolRequestContext
AiToolRoutingSupport
```

`AiToolRoutingSupport` contains the intent helpers that were previously private methods in `AiToolCallingService`. They are protected so handlers can reuse the exact same predicates without duplicating logic.

## Behavior intentionally unchanged

This phase preserves:

- same routing order
- same intent predicates
- same read-only guard behavior
- same admin-only behavior
- same tool answers
- same lack of Tool -> RAG fallback for now
- same lack of LLM-driven planning for now

## Why `AiReadOnlyToolService` is not split yet

`AiReadOnlyToolService` still acts as the read-only query facade. Splitting it into repositories/query services is the next safe step after validating this handler split.

Recommended next phase:

```text
9P-C — Split read-only tool queries by domain and add Tool/RAG fallback statuses
```

## Validation

Run backend tests:

```bash
cd backend
./mvnw test
```

Then run the smoke test checklist in `docs/47-ai-orchestration-smoke-test-plan.md`.
