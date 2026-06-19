# 14A — Product Box Models backend foundation

Status: **Completed / backend metadata MVP**

## Purpose

Create the backend foundation for Product Box Models without images and without frontend changes. This phase is intentionally small and safe. It only introduces metadata CRUD for rectangular product boxes.

## Scope implemented

Included:

- Flyway migration for `product_box_models`.
- New backend module package: `com.tamias.productbox`.
- JPA entity.
- Repository.
- DTOs.
- Mapper.
- Service.
- Controller.
- Validation.
- Organization scoping.
- Optional association with `inventory_items`.
- Optional association with `purchase_items`.
- Soft delete for the parent model.

Not included:

- No face images.
- No S3 upload/delete.
- No presigned URLs.
- No Angular UI.
- No Three.js.
- No AI tools.

## Migration

Implemented as:

```text
backend/src/main/resources/db/migration/V32__create_product_box_models.sql
```

The previous migration in the repository was:

```text
V31__create_ai_chat_message_debugs.sql
```

## Table

```sql
CREATE TABLE product_box_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    inventory_item_id UUID NULL,
    purchase_item_id UUID NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    width NUMERIC(10, 2) NOT NULL,
    height NUMERIC(10, 2) NOT NULL,
    depth NUMERIC(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'cm',
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL
);
```

Validation constraints:

```text
width > 0
height > 0
depth > 0
unit IN ('cm', 'mm', 'in')
```

No `at_least_one_parent` constraint was added because standalone models are allowed in 14A. This keeps the MVP flexible and lets the UI create a model first and associate it later if needed.

## Backend package

Implemented under:

```text
backend/src/main/java/com/tamias/productbox/
```

Files:

```text
productbox/controller/ProductBoxModelController.java
productbox/dto/ProductBoxModelRequest.java
productbox/dto/ProductBoxModelResponse.java
productbox/entity/ProductBoxModel.java
productbox/enums/ProductBoxUnit.java
productbox/enums/ProductBoxUnitConverter.java
productbox/mapper/ProductBoxModelMapper.java
productbox/repository/ProductBoxModelRepository.java
productbox/service/ProductBoxModelService.java
```

Also updated:

```text
purchase/repository/PurchaseItemRepository.java
```

Reason: 14A needs a safe organization-scoped lookup for purchase items whose parent purchase list is not soft-deleted.

## API

```text
GET    /api/v1/product-box-models
GET    /api/v1/product-box-models/{id}
POST   /api/v1/product-box-models
PUT    /api/v1/product-box-models/{id}
DELETE /api/v1/product-box-models/{id}
```

The list endpoint supports filters:

```text
inventoryItemId
purchaseItemId
search
page/size/sort via Pageable
```

14A does not add nested inventory/purchase routes yet. The filters are enough for the backend foundation and keep the API surface small.

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

Valid units accepted by API:

```text
cm
mm
in
```

The Java enum uses uppercase constants internally and serializes/deserializes lowercase values through `ProductBoxUnit` + `ProductBoxUnitConverter`.

## Response DTO for 14A

Faces are intentionally not included yet.

```json
{
  "id": "...",
  "name": "Caja cafetera Oster",
  "description": "Caja original del producto",
  "inventoryItemId": "...",
  "inventoryItemName": "Cafetera",
  "inventoryItemBrandId": "...",
  "inventoryItemBrandName": "Oster",
  "purchaseItemId": null,
  "purchaseItemNameSnapshot": null,
  "purchaseListId": null,
  "width": 30,
  "height": 40,
  "depth": 20,
  "unit": "cm",
  "createdAt": "...",
  "updatedAt": "..."
}
```

## Validation rules

- `name` required.
- `name` max 255 characters.
- `width > 0`.
- `height > 0`.
- `depth > 0`.
- `unit` required.
- Valid units: `cm`, `mm`, `in`.
- If `inventoryItemId` is provided, it must belong to the current organization and not be soft-deleted.
- If `purchaseItemId` is provided, it must belong to the current organization and its purchase list must not be soft-deleted.
- If both `inventoryItemId` and `purchaseItemId` are provided and the purchase item already points to a different inventory item, the request is rejected.

## Security and tenancy

Every query filters by:

```text
organization_id = currentUserService.getCurrentOrganizationId()
```

The frontend never sends or controls `organizationId`.

Service permissions:

```text
Read:  ADMINISTRATOR, PROPERTY_MANAGER, MAINTENANCE_STAFF, READ_ONLY
Write: ADMINISTRATOR, PROPERTY_MANAGER, MAINTENANCE_STAFF
Delete: ADMINISTRATOR, PROPERTY_MANAGER
```

## Delete behavior in 14A

14A uses soft delete for the parent model:

```text
product_box_models.deleted_at
product_box_models.deleted_by
```

This phase has no face images yet, so there is no S3 cleanup in 14A.

In 14B, deleting a model must delete all face S3 objects first and then delete/hide the model according to the policy documented in `71-product-box-models-14.md`.

## Manual tests

1. Create a standalone product box model.
2. Create a product box model linked to an inventory item.
3. Create a product box model linked to a purchase item.
4. List all product box models.
5. Filter by `inventoryItemId`.
6. Filter by `purchaseItemId`.
7. Search by model name.
8. Search by inventory item name.
9. Search by purchase item snapshot.
10. Verify another organization cannot fetch/update/delete the record.
11. Update dimensions.
12. Reject zero/negative dimensions.
13. Reject invalid unit.
14. Delete model and confirm it no longer appears in list/detail.

## Commands

```bash
cd backend
./mvnw test
```
