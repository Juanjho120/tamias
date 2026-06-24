# 15F — AI Read-only Tool Support Decomposition

## Status

Implemented.

## Goal

Reduce the maintenance risk of the AI read-only tools by decomposing the large `AiReadOnlyToolSupport` base class before starting Payments and Reports.

Before this phase, `AiReadOnlyToolSupport` had grown into a monolithic support class that mixed query helpers, formatting helpers, search normalization, access helpers and domain-specific read helpers for several AI tools. The goal of 15F is to keep the existing AI behavior stable while making the support layer easier to maintain and extend.

## Scope

### Backend

- Keep the public inheritance entry point as `AiReadOnlyToolSupport` so existing AI repositories continue to compile without broad repository rewrites.
- Split shared read-only AI support responsibilities into smaller support classes under:

```text
backend/src/main/java/com/tamias/ai/tool/support
```

- Preserve the existing AI tool behavior, routing, prompts and response style.
- Preserve organization-scoped read-only behavior based on the current authenticated organization context.
- Preserve `SUPER_ADMIN`-aware access helpers for admin-oriented AI tools.
- Avoid adding Payments or Reports logic to this phase.

### No scope

- No database schema changes.
- No Flyway migration.
- No frontend changes.
- No translation changes.
- No AI behavior change intended.
- No new write-capable AI tools.

## Resulting support areas

The read-only support layer is decomposed into smaller responsibilities:

```text
AiBaseReadOnlyToolSupport
AiReadOnlyQuerySupport
AiToolSearchSupport
AiToolFormattingSupport
AiScheduledReservationReadSupport
AiDocumentReadSupport
AiReservationSupplyTaskReadSupport
AiMaintenancePropertyCatalogReadSupport
AiPurchaseReadSupport
AiFileImageReadSupport
AiChatHistoryReadSupport
AiToolAccessSupport
AiReadOnlyToolSupport
```

### Responsibility split

```text
AiBaseReadOnlyToolSupport
- Shared EntityManager and CurrentUserService access.
- Shared constants and base context.

AiReadOnlyQuerySupport
- Native query helpers.
- Scalar query helpers.
- Query value normalization.

AiToolSearchSupport
- Search text extraction.
- Stop words.
- Text normalization helpers.

AiToolFormattingSupport
- Common display formatting.
- Money, bytes, booleans, dates and names.
- Timeline and row formatting helpers.

AiScheduledReservationReadSupport
- Scheduled maintenance read helpers.
- Reservation list/read helpers.

AiDocumentReadSupport
- Document metadata rows and document answer helpers.

AiReservationSupplyTaskReadSupport
- Reservation supply rows.
- Reservation supply summary rows.
- Task list rows.

AiMaintenancePropertyCatalogReadSupport
- Maintenance rows and image helper rows.
- Property/catalog-oriented read helpers shared by tools.

AiPurchaseReadSupport
- Purchase-oriented read helpers shared by purchase tools.

AiFileImageReadSupport
- File/image metadata read helpers shared by dashboard/image tools.

AiChatHistoryReadSupport
- AI chat history read helpers.

AiToolAccessSupport
- Administrator and SUPER_ADMIN checks.
- Admin-only denial response.
- Current user access rows.
- User/role rows and permission summary helpers.

AiReadOnlyToolSupport
- Thin compatibility entry point for existing AI repositories.
```

## Design decisions

- Existing AI repositories should continue extending `AiReadOnlyToolSupport`.
- The decomposition should not force a broad rewrite of all repositories in this phase.
- The split is intentionally internal to the support layer.
- Payments and Reports should build on this smaller support structure instead of growing a new monolithic helper.
- `SUPER_ADMIN` handling must remain available to user, role and organization AI tools.

## Verification checklist

After applying this refactor, verify that existing AI tools still work with no behavior regression:

```text
¿Qué necesita atención hoy?
Dame el resumen operativo
Qué documentos están listos para IA?
Qué modelos 3D están incompletos?
Qué usuarios son SUPER_ADMIN?
Qué accesos tengo?
Qué reservaciones tengo esta semana?
Qué mantenimientos programados están vencidos?
Qué supplies se usaron en la última reserva?
```

Also verify backend compilation/tests:

```bash
cd backend
mvnw.cmd test
```

## Result

15F prepares the AI support layer for future operational modules. The next active implementation phase is Payments.

```text
16 Payments
17 Reports
18 Notifications and reminders
19 Blueprint Analysis
```
