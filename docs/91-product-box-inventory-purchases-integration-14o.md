# 14O — Product Box Integration with Inventory/Purchases

## Status

Completed.

## Goal

Integrate Product Box Models into the existing Inventory Items and Purchase Items workflows so users can move from operational records into the Product Box module without manually copying IDs.

The Product Box backend already supports optional associations with:

- `inventory_item_id`
- `purchase_item_id`

Because those fields and filters already exist, this phase does not require a new Flyway migration.

## Scope

### Backend

No backend changes are required for this phase.

The existing backend contract already supports 14O:

```http
GET /api/v1/product-box-models?inventoryItemId=<uuid>
GET /api/v1/product-box-models?purchaseItemId=<uuid>
GET /api/v1/product-box-models?inventoryItemId=<uuid>&purchaseItemId=<uuid>
```

Product Box model creation and update already accept optional association fields through the existing request DTO:

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

### Frontend

This phase adds integration points in the existing Angular UI:

1. Inventory Items catalog
   - Shows a Product Box action only when the selected catalog is `inventory-items`.
   - Navigates to `/product-box-models?inventoryItemId=<inventory item id>`.
   - The Product Box page loads filtered models for that inventory item.
   - If the user clicks New Model from that filtered page, the form is prefilled with the selected inventory item.

2. Purchase Items modal
   - Shows a Product Box action for each purchase item.
   - Navigates to `/product-box-models?purchaseItemId=<purchase item id>`.
   - If the purchase item is linked to an inventory item, the navigation also includes `inventoryItemId`.
   - The Product Box page loads filtered models for that purchase item.
   - If the user clicks New Model from that filtered page, the form is prefilled with the purchase item and inventory item when available.

3. Product Box Models page
   - Reads initial `inventoryItemId`, `purchaseItemId` and `search` query params.
   - Applies those params to the existing filters.
   - Uses the active filters to prefill the create form.
   - Supports `create=true` for direct create flows without adding a separate endpoint.

## Files changed

```text
frontend/src/app/features/catalogs/pages/catalogs-page/catalogs-page.component.ts
frontend/src/app/features/catalogs/pages/catalogs-page/catalogs-page.component.html
frontend/src/app/features/purchases/components/purchase-items-modal/purchase-items-modal.component.ts
frontend/src/app/features/purchases/components/purchase-items-modal/purchase-items-modal.component.html
frontend/src/app/features/product-box-models/pages/product-box-models-page/product-box-models-page.component.ts
frontend/src/app/features/product-box-models/pages/product-box-models-page/product-box-models-page.component.html
frontend/src/app/features/product-box-models/components/product-box-model-form-modal/product-box-model-form-modal.component.ts
frontend/src/app/features/product-box-models/components/product-box-model-form-modal/product-box-model-form-modal.component.html
docs/ROADMAP.md
docs/91-product-box-inventory-purchases-integration-14o.md
```

## Verification checklist

### Inventory Item integration

1. Open Catalogs.
2. Select the Inventory Items catalog.
3. Click the Product Box action for an inventory item.
4. Confirm the app navigates to:

```text
/product-box-models?inventoryItemId=<inventory item id>
```

5. Confirm the Product Box list is filtered by that inventory item.
6. Click New Model.
7. Confirm the Inventory Item field is prefilled.
8. Save a model and confirm it remains associated with that inventory item.

### Purchase Item integration

1. Open Purchases.
2. Open the Items modal for a purchase list.
3. Click the Product Box action for a purchase item.
4. Confirm the app navigates to:

```text
/product-box-models?purchaseItemId=<purchase item id>
```

If the purchase item is linked to an inventory item, confirm the URL also includes:

```text
inventoryItemId=<inventory item id>
```

5. Confirm the Product Box list is filtered by that purchase item.
6. Click New Model.
7. Confirm the Purchase Item ID field is prefilled.
8. Save a model and confirm it remains associated with that purchase item.

### Regression checks

- Existing Product Box CRUD still works without query params.
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
