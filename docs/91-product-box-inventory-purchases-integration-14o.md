# 14O — Product Box Integration with Inventory/Purchases

## Status

Completed.

## Goal

Integrate Product Box Models into the existing Inventory Items and Purchase Items workflows so users can create a Product Box Model from the operational record without manually copying IDs or manually filling association fields.

The Product Box backend already supports optional associations with:

- `inventory_item_id`
- `purchase_item_id`

Because those fields already exist and model creation already accepts those optional association IDs, this phase does not require a new Flyway migration.

## Scope

### Backend

No backend changes are required for this phase.

The existing Product Box model creation/update contract already accepts optional association fields through the existing request DTO:

```json
{
  "inventoryItemId": "optional UUID",
  "purchaseItemId": "optional UUID",
  "name": "Model name",
  "description": "Optional description",
  "width": 10,
  "height": 20,
  "depth": 5,
  "unit": "cm"
}
```

The existing list filters are still supported for normal Product Box browsing:

```http
GET /api/v1/product-box-models?inventoryItemId=
GET /api/v1/product-box-models?purchaseItemId=
GET /api/v1/product-box-models?inventoryItemId=&purchaseItemId=
```

### Frontend

This phase adds direct create integration points in the existing Angular UI.

#### Inventory Items catalog

- Shows a Product Box action only when the selected catalog is `inventory-items`.
- Navigates to `/product-box-models?inventoryItemId=&create=true`.
- The Product Box page opens the create modal automatically.
- The Product Box page uses the query param only to preselect the Inventory Item in the form.
- The query param is not copied into the visible Product Box search filters when `create=true` is present.

#### Purchase Items modal

- Shows a Product Box action for each purchase item.
- Navigates to `/product-box-models?purchaseItemId=&create=true`.
- If the purchase item is linked to an inventory item, the navigation also includes `inventoryItemId`.
- The Product Box page opens the create modal automatically.
- The Purchase Item UUID is prefilled in the create form.
- The Inventory Item is preselected in the create form when available.
- The query params are not copied into the visible Product Box search filters when `create=true` is present.

#### Product Box Models page

- Continues to support normal filter deep links without `create=true`.
- When `create=true` is present, query params are treated as initial form associations, not as search filters.
- The normal New Model button still uses any active visible filters as initial form associations.

## Files changed

```text
frontend/src/app/features/catalogs/pages/catalogs-page/catalogs-page.component.ts
frontend/src/app/features/purchases/components/purchase-items-modal/purchase-items-modal.component.ts
frontend/src/app/features/product-box-models/pages/product-box-models-page/product-box-models-page.component.ts
docs/91-product-box-inventory-purchases-integration-14o.md
```

## Verification checklist

### Inventory Item direct create integration

1. Open Catalogs.
2. Select the Inventory Items catalog.
3. Click the Product Box action for an inventory item.
4. Confirm the app navigates to:

```text
/product-box-models?inventoryItemId=<inventoryItemId>&create=true
```

5. Confirm the Product Box create modal opens automatically.
6. Confirm the Inventory Item field is preselected.
7. Confirm the visible Product Box filters are not filled with the inventory item.
8. Save a model and confirm it remains associated with that inventory item.

### Purchase Item direct create integration

1. Open Purchases.
2. Open the Items modal for a purchase list.
3. Click the Product Box action for a purchase item.
4. Confirm the app navigates to:

```text
/product-box-models?purchaseItemId=<purchaseItemId>&create=true
```

If the purchase item is linked to an inventory item, confirm the URL also includes:

```text
inventoryItemId=<inventoryItemId>
```

5. Confirm the Product Box create modal opens automatically.
6. Confirm the Purchase Item ID field is prefilled.
7. Confirm the Inventory Item field is preselected when available.
8. Confirm the visible Product Box filters are not filled with the purchase item or inventory item.
9. Save a model and confirm it remains associated with that purchase item.

### Regression checks

- Existing Product Box CRUD still works without query params.
- Existing Product Box list filters still work when navigating without `create=true`.
- Existing Product Box face upload still works.
- Existing Product Box Three.js viewer still works.
- Existing Inventory Item images modal still opens from the Inventory Items catalog.
- Existing Purchase Items create/edit/delete/purchased flows still work.

## Suggested commands

Frontend:

```bash
cd frontend
npm start
```

Frontend build:

```bash
cd frontend
npm run build
```

Backend smoke check, if needed:

```bash
cd backend
./mvnw test
```

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd test
```
