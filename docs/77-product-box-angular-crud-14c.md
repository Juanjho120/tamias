# 14C — Angular Product Box CRUD

Status: Implemented in this phase.

## Goal

Add the first frontend management screen for Product Box Models without introducing Three.js yet.

This phase connects Angular to the backend delivered in 14A and 14B:

- Product Box Model metadata CRUD.
- Per-face image upload, replacement and deletion.
- Presigned URL previews returned by the backend.
- Navigation entry and protected route.

The Three.js viewer remains intentionally deferred to 14D.

## Scope

Included:

- New Angular feature folder: `features/product-box-models`.
- Product Box Models list page.
- Create/edit modal for metadata.
- Delete confirmation with `ConfirmModalComponent`.
- Inventory item selector based on active inventory items.
- Manual purchase item UUID field as an MVP bridge until 14E.
- Face images modal for `front`, `back`, `left`, `right`, `top`, `bottom`.
- File picker constrained to JPG, PNG and WEBP.
- Image previews using presigned URLs returned by the backend.
- Navigation route `/product-box-models`.

Not included:

- Three.js rendering.
- OrbitControls.
- GLB/GLTF export.
- AI tools.
- Purchase module visual integration.
- Inventory item detail integration.

## Backend dependencies

This phase assumes these backend endpoints already exist:

```text
GET    /api/v1/product-box-models
GET    /api/v1/product-box-models/{id}
POST   /api/v1/product-box-models
PUT    /api/v1/product-box-models/{id}
DELETE /api/v1/product-box-models/{id}

GET    /api/v1/product-box-models/{id}/faces
GET    /api/v1/product-box-models/{id}/faces/{faceName}
POST   /api/v1/product-box-models/{id}/faces/{faceName}
PUT    /api/v1/product-box-models/{id}/faces/{faceName}
DELETE /api/v1/product-box-models/{id}/faces/{faceName}
```

## Frontend decisions

### No Three.js in 14C

14C does not install or import `three`. This prevents unnecessary bundle growth before the viewer phase.

### Face images

Face image operations are immediate per face:

- Upload if the face has no image.
- Replace if the face already has an image.
- Delete with a confirmation modal.

The backend remains responsible for hard deleting old/replaced/deleted face images from S3.

### Purchase item association

14C exposes a manual `purchaseItemId` UUID field because the current frontend does not yet have a generic purchase item selector outside purchase list detail flows.

A visual selector and create-from-purchase workflow are deferred to 14E.

## Testing

Run:

```bash
cd frontend
npm run build
```

Manual tests:

1. Open Product Box Models from the sidebar.
2. Create a standalone model.
3. Create a model linked to an inventory item.
4. Edit dimensions and unit.
5. Upload images for several faces.
6. Replace a face image.
7. Delete a face image.
8. Delete the model and confirm that backend deletes S3 face images before soft-deleting the parent.
