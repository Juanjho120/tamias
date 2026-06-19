# 14A — Product Box Models backend foundation

Status: **Planned / implementation next**

## Purpose

Create the backend foundation for Product Box Models without images and without frontend changes.

This phase is intentionally small and safe. It only introduces metadata CRUD for rectangular product boxes.

## Scope

Included:

- Flyway migration for `product_box_models`.
- JPA entity.
- Repository.
- DTOs.
- Mapper.
- Service.
- Controller.
- Validation.
- Organization scoping.
- Optional association with inventory item and purchase item.
- Soft delete for the parent model.

Not included:

- No face images.
- No S3 upload/delete.
- No presigned URLs.
- No Angular UI.
- No Three.js.
- No AI tools.

## Migration

Next migration should be based on the actual repository state at implementation time. After the current AI debug/smoke phases, the last known migration is `V31__create_ai_chat_message_debugs.sql`, so the expected next migration is likely:

```text
V32__create_product_box_models.sql
```

Before implementation, verify the migration folder again to avoid version collisions.

Recommended table:

```sql
CREATE TABLE product_box_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    inventory_item_id UUID NULL REFERENCES inventory_items(id),
    purchase_item_id UUID NULL REFERENCES purchase_items(id),
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    width NUMERIC(10,2) NOT NULL,
    height NUMERIC(10,2) NOT NULL,
    depth NUMERIC(10,2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'cm',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID NULL REFERENCES users(id),
    updated_at TIMESTAMP NULL,
    deleted_by UUID NULL REFERENCES users(id),
    deleted_at TIMESTAMP NULL,

    CONSTRAINT chk_product_box_models_width_positive CHECK (width > 0),
    CONSTRAINT chk_product_box_models_height_positive CHECK (height > 0),
    CONSTRAINT chk_product_box_models_depth_positive CHECK (depth > 0),
    CONSTRAINT chk_product_box_models_unit CHECK (unit IN ('cm', 'mm', 'in')),
    CONSTRAINT chk_product_box_models_at_least_one_parent CHECK (
        inventory_item_id IS NOT NULL OR purchase_item_id IS NOT NULL
    )
);

CREATE INDEX idx_product_box_models_organization_id
    ON product_box_models(organization_id);

CREATE INDEX idx_product_box_models_inventory_item_id
    ON product_box_models(inventory_item_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_product_box_models_purchase_item_id
    ON product_box_models(purchase_item_id)
    WHERE deleted_at IS NULL;
```

The `at_least_one_parent` check is optional. If the UI should allow a standalone model first and associate it later, skip that constraint. For the first implementation, allowing standalone models may be more flexible and safer.

## Backend package

Use a new module package:

```text
backend/src/main/java/com/tamias/productbox/
```

Suggested files:

```text
productbox/controller/ProductBoxModelController.java
productbox/dto/ProductBoxModelRequest.java
productbox/dto/ProductBoxModelResponse.java
productbox/entity/ProductBoxModel.java
productbox/enums/ProductBoxUnit.java
productbox/mapper/ProductBoxModelMapper.java
productbox/repository/ProductBoxModelRepository.java
productbox/service/ProductBoxModelService.java
```

## API

```text
GET    /api/v1/product-box-models
GET    /api/v1/product-box-models/{id}
POST   /api/v1/product-box-models
PUT    /api/v1/product-box-models/{id}
DELETE /api/v1/product-box-models/{id}
```

Optional lookup endpoints if useful in 14A:

```text
GET /api/v1/inventory-items/{id}/box-models
GET /api/v1/purchase-items/{id}/box-models
```

## Request DTO

```json
{
  "name": "Caja cafetera Oster",
  "description": "Caja original del producto",
  "inventoryItemId": "...",
  "purchaseItemId": null,
  "width": 30,
  "height": 40,
  "depth": 20,
  "unit": "cm"
}
```

## Response DTO for 14A

Do not include faces yet.

```json
{
  "id": "...",
  "name": "Caja cafetera Oster",
  "description": "Caja original del producto",
  "inventoryItemId": "...",
  "inventoryItemName": "Cafetera",
  "inventoryItemBrandName": "Oster",
  "purchaseItemId": null,
  "width": 30,
  "height": 40,
  "depth": 20,
  "unit": "cm",
  "createdAt": "...",
  "updatedAt": null
}
```

## Validation rules

- `name` required.
- `width > 0`.
- `height > 0`.
- `depth > 0`.
- `unit` required.
- Valid units: `cm`, `mm`, `in`.
- If `inventoryItemId` is provided, it must belong to the current organization and not be soft-deleted.
- If `purchaseItemId` is provided, it must belong to the current organization and not be soft-deleted.

## Security and tenancy

Every query must filter by:

```text
organization_id = currentUserService.getCurrentOrganizationId()
```

Do not trust organization id from the frontend.

## Delete behavior in 14A

Use soft delete for the parent model:

```text
product_box_models.deleted_at
product_box_models.deleted_by
```

This phase has no face images yet, so there is no S3 cleanup in 14A.

In 14B, deleting a model must delete all face S3 objects first and then delete/hide the model according to the policy documented in `71-product-box-models-14.md`.

## Manual tests

1. Create a standalone product box model.
2. Create a product box model linked to an inventory item.
3. Create a product box model linked to a purchase item if purchase item lookup is implemented.
4. Verify list endpoint only returns current organization records.
5. Verify another organization cannot fetch/update/delete the record.
6. Update dimensions.
7. Reject zero/negative dimensions.
8. Reject invalid unit.
9. Delete model and confirm it no longer appears in list/detail.

## Commands

```bash
cd backend
./mvnw test
```
