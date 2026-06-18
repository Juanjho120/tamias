# 12A — Associate Brands Directly with Inventory Items

## Purpose

Move brand ownership from purchase item lines to the shared Inventory Items catalog.

The catalog item now knows its brand, and purchases derive/display the brand from the selected inventory item.

---

## User decision

```text
Add brand_id to inventory_items.
Remove brand_id from purchase_items.
```

---

## Implemented model

### inventory_items

Added:

```text
brand_id nullable FK -> brands.id
```

Rules:

- Brand is optional.
- Brand must belong to the same organization when selected.
- Brand must not be deleted.
- Inventory item API responses expose `brandId` and `brandName`.

### purchase_items

Removed:

```text
brand_id
```

Rules:

- Purchase item requests no longer accept `brandId`.
- Purchase item responses still expose `brandId` and `brandName`, derived from `purchase_item.inventory_item.brand`.
- Purchase item forms no longer ask the user to select a brand.

---

## Data migration

Migration:

```text
V27__associate_brands_with_inventory_items.sql
```

Migration behavior:

```text
1. Add inventory_items.brand_id.
2. Backfill inventory_items.brand_id from purchase_items.brand_id when:
   - purchase_items.inventory_item_id is present
   - purchase_items.brand_id is present
   - the inventory item does not already have a brand
3. Use the most recent purchase item brand when multiple purchase items reference the same inventory item.
4. Drop purchase_items.brand_id.
5. Drop idx_purchase_items_brand.
6. Drop fk_purchase_items_brand.
```

If migrated dev data has conflicts, the inventory item keeps its current brand and can be corrected manually from the Inventory Items catalog.

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

This phase updates:

```text
Inventory Items catalog
Purchase list form item selector
Purchase items modal item selector
Purchase item display labels
```

Future phases/screens should reuse the same display rule when item selectors are touched:

```text
Maintenance item usage
Reservation supplies
AI tool answer formatting
```

---

## Backend changes

Implemented:

```text
InventoryItem.brand
InventoryItemRequest.brandId
InventoryItemResponse.brandId
InventoryItemResponse.brandName
InventoryItemRepository brand-aware search
InventoryItemService brand validation
PurchaseItem no longer stores brand
PurchaseItemRequest without brandId
PurchaseItemUpdateRequest without brandId
PurchaseMapper derives brand from inventory item
PurchaseListService no longer resolves brand from purchase items
```

---

## Acceptance tests

```text
1. Create an inventory item without brand.
2. Create an inventory item with brand.
3. Edit inventory item brand.
4. Search inventory item by brand name.
5. Confirm inventory item label shows item - brand.
6. Add purchase item using inventory item.
7. Confirm purchase item displays brand from inventory item.
8. Confirm purchase item form no longer asks for brand.
9. Confirm purchase_items.brand_id no longer exists after migration.
```

---

## Out of scope

- Inventory stock control.
- Brand images.
- Supplier preferred brand rules.
- Inventory item images; that belongs to 12B.
- Purchase list images; that belongs to 12C.
- Reservation images; that belongs to 12D.
