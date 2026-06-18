# 12A — Associate Brands Directly with Inventory Items

## Purpose

Move brand ownership from purchase item lines to the shared Inventory Items catalog.

The catalog item should know its brand, and purchases should derive the brand from the selected inventory item.

---

## User decision

```text
Add brand_id to inventory_items.
Remove brand_id from purchase_items.
```

---

## Target model

### inventory_items

Add:

```text
brand_id nullable FK -> brands.id
```

Rules:

- Brand is optional.
- Brand must belong to the same organization when selected.
- Brand must be active/not deleted according to current catalog rules.

### purchase_items

Remove:

```text
brand_id
```

Rules:

- Purchase item displays brand from `inventory_item.brand`.
- If historical purchase item snapshots exist, review whether a brand snapshot is needed before removing data.
- Do not remove fields blindly. Inspect current migrations, entity and DTOs.

---

## UI display rule

All inventory item search/select labels should use:

```text
{item name} - {brand}
```

When brand is missing:

```text
{item name}
```

Examples:

```text
Jabón de Baño - Dove
Covertor Elástico - Spring Air
Cloro
```

This applies to item selectors in:

```text
Inventory Items catalog
Maintenance item usage
Reservation supplies
Purchase list items
Any frontend item search component
```

---

## Backend changes

Before implementing, inspect:

```text
InventoryItem entity
Brand entity
PurchaseItem entity
InventoryItemRequest/Response DTOs
PurchaseItemRequest/Response DTOs
Repositories
Flyway migrations
AI tool repositories that list items/purchases/supplies
```

Expected API changes:

```text
InventoryItemRequest.brandId
InventoryItemResponse.brandId
InventoryItemResponse.brandName
```

Purchase item request should no longer accept brand id after migration.

---

## Data migration considerations

If existing `purchase_items.brand_id` has data, decide migration behavior before dropping it.

Possible approach:

```text
1. Add inventory_items.brand_id nullable.
2. For purchase items with inventory_item_id and brand_id, backfill inventory_items.brand_id only when inventory item has no brand yet.
3. Resolve conflicts manually or leave inventory item brand unchanged.
4. Remove purchase_items.brand_id.
```

For local/dev MVP data, manual correction may be acceptable, but migration should still be safe.

---

## AI tool impact

Update tools that display inventory items, purchase items or reservation supplies so they include brand where useful.

Examples:

```text
¿Qué supplies necesito para la próxima reserva?
¿Cuándo compré por última vez cloro?
¿Qué item compro más seguido?
¿Qué items se usan más?
```

Use the item display name consistently:

```text
itemNameWithBrand = itemName + optional brand
```

---

## Acceptance tests

```text
1. Create an inventory item with brand.
2. Edit inventory item brand.
3. Search inventory item and confirm label shows item - brand.
4. Add purchase item using inventory item.
5. Confirm purchase item displays brand from inventory item.
6. Confirm purchase item form no longer asks for brand.
7. Add reservation supply and confirm item selector shows brand.
8. Add maintenance record item and confirm item selector shows brand.
9. Ask AI about purchases/supplies and confirm brand appears when relevant.
```

---

## Out of scope

- Inventory stock control.
- Brand images.
- Supplier preferred brand rules.
