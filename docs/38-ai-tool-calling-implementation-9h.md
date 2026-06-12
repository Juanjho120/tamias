# 38 — AI Tool Calling Implementation 9H

## Phase

9H — Inventory and maintenance analytics.

## Scope

This phase expands the read-only AI tool catalog with inventory usage and maintenance analytics.

The implementation remains aligned with the approved Tool Calling constraints:

- Read-only first.
- No free SQL from the model.
- No autonomous writes.
- Backend-owned `organization_id` resolution.
- Backend-owned current user resolution.
- Tool evidence returned to the frontend.
- No private file URLs exposed.

## Added Inventory Tools

- `inventory.search`
- `inventory.getFrequentlyUsed`
- `inventory.getUnusedItems`
- `inventory.getItemsUsedInReservations`
- `inventory.getItemsUsedInPurchases`
- `inventory.getItemsUsedInMaintenance`

Supported examples:

- ¿Qué items tengo registrados?
- ¿Qué supplies se usan más en reservaciones?
- ¿Qué items nunca he usado?
- ¿Dónde se ha usado el café?
- ¿Qué items se usaron en mantenimientos?

## Added Maintenance Analytics Tools

- `maintenance.search`
- `maintenance.recent`
- `maintenance.byProperty`
- `maintenance.byCategoryOrType`
- `maintenance.byStatus`
- `maintenance.costSummary`
- `maintenance.costByProperty`
- `maintenance.costByCategory`
- `maintenance.costByMonth`
- `maintenance.withImages`
- `maintenance.withoutImages`

Supported examples:

- ¿Cuánto gasté en mantenimiento este mes?
- ¿Qué mantenimientos tienen evidencia fotográfica?
- ¿Qué mantenimientos no tienen imágenes?
- ¿Cuáles fueron los mantenimientos más caros?
- ¿Qué items se usaron en mantenimientos?
- ¿Qué mantenimientos completados tengo?

## Schema notes

The implementation uses the current database structure:

- `inventory_items`
- `reservation_supplies`
- `purchase_items`
- `purchase_lists`
- `maintenance_records`
- `maintenance_record_items`
- `maintenance_record_images`
- `maintenance_categories`
- `maintenance_types`
- `properties`

It does not depend on old `materials` names. The migration `V20__refactor_materials_to_inventory_items.sql` renamed old material-related structures into inventory item structures.

## Security notes

All queries are scoped by the authenticated user's current organization via `CurrentUserService.getCurrentOrganizationId()`.

No tool accepts SQL from the LLM or from the user.

## Smoke tests

Use the AI Assistant UI and test:

```text
¿Qué items tengo registrados?
¿Qué items se usan más?
¿Qué items nunca he usado?
¿Qué supplies se usan más en reservaciones?
¿Dónde se ha usado el café?
¿Qué items se usaron en mantenimientos?
¿Cuánto gasté en mantenimiento este mes?
¿Qué propiedad tiene más gastos de mantenimiento?
¿Cuánto gasté en mantenimiento por categoría?
¿Cuánto gasté en mantenimiento por mes?
¿Qué mantenimientos tienen evidencia fotográfica?
¿Qué mantenimientos no tienen imágenes?
¿Qué mantenimientos completados tengo?
```
