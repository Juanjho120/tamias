# Phase 9K — Purchase analytics AI tools

This phase expands TAMIAS AI Tool Calling with read-only purchase analytics.

## Scope

Implemented backend-owned, read-only tools for purchase lists and purchase items.

### Purchase List Tools

- `purchaseList.search`
- `purchaseList.byProperty`
- `purchaseList.byDateRange`
- `purchaseList.recent`
- `purchaseList.pending`
- `purchaseList.completed`
- `purchaseList.costSummary`
- `purchaseList.costByProperty`
- `purchaseList.costByCategory`
- `purchaseList.costByMonth`

### Purchase Item Tools

- `purchaseItem.search`
- `purchaseItem.byPurchaseList`
- `purchaseItem.byInventoryItem`
- `purchaseItem.lastPurchased`
- `purchaseItem.priceHistory`
- `purchaseItem.averageUnitCost`
- `purchaseItem.quantitySummary`
- `purchaseItem.mostPurchased`
- `purchaseItem.leastPurchased`
- `purchaseItem.costTrend`

## Safety model

- Read-only queries only.
- No autonomous writes.
- No model-generated SQL.
- All queries are owned by the backend.
- All results are scoped by `organization_id` from `CurrentUserService`.
- Deleted purchase lists are excluded with `deleted_at IS NULL`.
- Purchase analytics that represent real spending use only `purchase_items.purchased = TRUE`.

## Schema notes

The implementation uses the current schema:

- `purchase_lists.property_id`
- `purchase_lists.supplier_id`
- `purchase_lists.purchase_date`
- `purchase_lists.status`
- `purchase_items.inventory_item_id`
- `purchase_items.item_name_snapshot`
- `purchase_items.quantity`
- `purchase_items.unit`
- `purchase_items.estimated_price`
- `purchase_items.purchased`

The old `material_id` field is not used.

## Example prompts

- ¿Qué compras hice este mes?
- ¿Cuánto gasté en supplies?
- ¿Qué listas de compras están pendientes?
- ¿Qué propiedad generó más compras?
- ¿Cuándo compré café por última vez?
- ¿Cuánto cuesta normalmente el papel higiénico?
- ¿Qué item compro más seguido?
- ¿Ha subido el precio de algún producto?

## Notes

`estimated_price` is treated as the stored purchase item price value. For unit-cost analytics, the tool calculates `estimated_price / quantity` when quantity is available and greater than zero.
