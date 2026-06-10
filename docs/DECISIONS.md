# TAMIAS — Architecture and Technical Decisions

This document records important technical decisions for TAMIAS.

---

## 1. Architecture style

Decision:

```text
Use Modular Monolith for the MVP.
```

Reason:

- Lower operational complexity.
- Faster development.
- Easier deployment.
- Easier testing.
- Enough for the expected early scale.
- Still allows clean separation by domain modules.

---

## 2. Multi-tenancy strategy

Decision:

```text
Shared database + shared schema + organization_id
```

Reason:

- Simpler MVP.
- Lower cost.
- Easier reporting.
- Easier deployment.
- Good fit for early SaaS stage.

Rule:

> The backend resolves organization from authenticated user context. The frontend is not trusted as source of truth for organization isolation.

---

## 3. Primary keys

Decision:

```text
Use UUID primary keys.
```

Reason:

- Avoid sequential ID enumeration.
- Better for SaaS and future integrations.
- Suitable for distributed-friendly designs.

---

## 4. Backend framework

Decision:

```text
Use Java 21 + Spring Boot 3.
```

Reason:

- Strong backend stack for portfolio.
- Good ecosystem for security, JPA, validation, scheduling, S3 and AI.
- Aligns with target professional experience.

---

## 5. Frontend framework

Decision:

```text
Use Angular + TypeScript + Bootstrap.
```

Reason:

- Good fit for admin dashboards.
- Strong typed frontend.
- Reactive Forms.
- Fast UI development with Bootstrap.
- Relevant professional stack.

---

## 6. Database migration

Decision:

```text
Use Flyway.
```

Reason:

- Explicit schema evolution.
- Versioned migrations.
- Good fit with PostgreSQL and Spring Boot.

Rule:

> Do not modify migrations already applied in shared environments. Create new migrations for changes.

---

## 7. File storage

Decision:

```text
Use AWS S3 with private buckets and pre-signed URLs.
```

Reason:

- Avoid storing files in the backend filesystem.
- Production-friendly.
- Secure temporary access.
- Good portfolio value.

---

## 8. AI MVP

Decision:

```text
Start with RAG over documents.
```

Reason:

- Clear business value.
- Lower risk than unrestricted database agents.
- Allows document-grounded answers.
- Fits house rules, manuals, property documents and policies.

Rules:

- AI answers must be grounded in available documents.
- AI must say when information is not found.
- AI must not invent property rules.
- AI must respect organization isolation.
- No free SQL execution by AI.

---

## 9. Inventory Items refactor

Decision:

```text
Replace the old materials catalog with Inventory Items.
```

Reason:

The old `materials` concept was too narrow. TAMIAS needs one shared operational item catalog that can support:

- maintenance usage,
- purchase lists,
- reservation supplies,
- future inventory reporting,
- future AI tool calling.

Current model:

```text
inventory_items
```

Business label:

```text
Spanish: Insumos y materiales
English: Inventory Items
```

The item type is represented by:

```text
MATERIAL
SUPPLY
AMENITY
CLEANING_SUPPLY
TOOL
OTHER
```

Operational availability is controlled by:

```text
available_for_maintenance
available_for_reservations
available_for_purchases
```

---

## 10. Maintenance item usage

Decision:

```text
Use maintenance_record_items.
```

Reason:

- Avoid legacy naming tied only to materials.
- Support any Inventory Item type.
- Preserve item snapshots for history.
- Better fit for reports and AI queries.

---

## 11. Purchase items

Decision:

```text
Purchase items reference inventory_items through inventory_item_id.
```

Reason:

- Shared item catalog.
- Better analytics.
- Future inventory stock support.
- Cleaner frontend/backend naming.

---

## 12. Reservation supplies

Decision:

```text
Use reservation_supplies for items delivered to guests during reservations.
```

Reason:

- Supplies are operationally different from reservation header data.
- Supplies may be added/edited after reservation creation.
- Supplies should be reportable.
- Supplies should be queryable by future AI tools.

Frontend decision:

```text
Use a separate Supplies modal from the Reservations table.
```

Reason:

- Keeps the main reservation form clean.
- Matches the Purchases Items modal pattern.
- Allows focused add/edit/delete behavior.

---

## 13. Frontend legacy cleanup

Decision:

```text
Remove frontend material aliases after the Inventory Items migration.
```

Reason:

- Avoid technical debt.
- Prevent confusion between old materials and current inventory items.
- Keep UI/services/models aligned with backend domain language.

---

## 14. Reports

Decision:

```text
Do not implement JasperReports in the MVP.
```

Reason:

- More valuable to first complete stable operational flows.
- Reports can build on existing clean data later.
- Dashboard analytics should come before full PDF reporting.

---

## 15. Tool Calling

Decision:

```text
Tool Calling will be added after MVP hardening and analytics foundations.
```

Reason:

- Tool calling should run over stable domain services.
- Read-only tools should come first.
- No SQL should be exposed directly to the model.

Initial tool candidates:

```text
findLastPurchaseByInventoryItem(itemName)
getMaintenanceCostByYear(year)
findOverdueTasks()
findLastMaintenanceByCategory(categoryName)
getUpcomingScheduledMaintenance(days)
getReservationSuppliesByReservation(reservationCode)
```
