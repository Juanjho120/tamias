# 12D — Reservation Images

Status: Completed.

## Purpose

Allow multiple images to be attached to a reservation.

Examples:

- check-in evidence
- special guest setup
- reservation-related damages
- parking/access evidence
- before/after photos

## Dependency

This phase depends on:

- 11A — S3 key strategy + filepath fields
- 11B — Hard delete policy for entity images

## Database

Created table:

```text
reservation_images
```

Required logical fields:

```text
id
organization_id
reservation_id
s3_key
filepath
filename/original_filename
content_type
size_bytes
created_at
created_by
```

Rules:

- Do not add soft-delete columns.
- Use hard delete only.
- Before implementing or changing this module, inspect the actual reservation entity/table and existing image entity patterns.

## S3 path

Use:

```text
{organizationId}/reservations/{reservationId}/{filename}
```

Example:

```text
s3_key: 5b8df6e1-d28c-493e-9840-c8c7a992d43f/reservations/73d8f9d3-4f6a-4e4a-86be-d89a9b58b98e/checkin.jpg
filepath: tamias-dev-files/5b8df6e1-d28c-493e-9840-c8c7a992d43f/reservations/73d8f9d3-4f6a-4e4a-86be-d89a9b58b98e
```

Bucket must come from configuration.

## Backend API

Endpoint pattern:

```text
GET    /api/v1/reservations/{reservationId}/images
GET    /api/v1/reservations/{reservationId}/images/{imageId}
POST   /api/v1/reservations/{reservationId}/images
GET    /api/v1/reservations/{reservationId}/images/{imageId}/file
DELETE /api/v1/reservations/{reservationId}/images/{imageId}
```

Rules:

- Parent reservation must belong to current organization.
- Delete must remove S3/storage object and DB row physically.
- If storage delete fails, DB row must not be deleted.
- Do not allow image access across organizations.
- Use hard delete only.
- Do not add image soft-delete fields.

## Frontend

Reservations screen includes an images action.

UI pattern:

```text
Reservations table
Images button
opens modal
list existing images
upload one or more images
delete image through app ConfirmModalComponent
```

The implemented modal must follow the same UX pattern used by properties and maintenance images:

- Selected image preview before upload.
- Clear button to reset selected files.
- Upload button aligned with Clear on the right side of the modal.
- Upload button uses the Bootstrap upload icon: `bi bi-upload`.
- Delete confirmation uses `ConfirmModalComponent`, not native browser confirmation dialogs.
- File input must restrict the picker to JPG, PNG and WEBP:

```html
accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
```

All visible labels and messages must use the current static i18n strategy:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

Translations for this modal live under:

```text
reservations.images
```

Do not hardcode Spanish or English UI text in components/templates.

## AI impact

AI image metadata tools were implemented later in 12E. The relevant tool is:

```text
images.getReservationImages
```

It supports questions such as:

```text
¿Qué reservaciones tienen imágenes?
¿Qué reservaciones no tienen fotos?
¿Qué imágenes tiene la reservación BOOK0098798?
```

## Acceptance tests

1. Open Reservations screen.
2. Click Images for a reservation.
3. Upload several images.
4. Confirm images appear in modal.
5. Confirm selected files show previews before upload.
6. Confirm Clear resets selected files and previews.
7. Confirm the file picker filters JPG, PNG and WEBP.
8. Confirm `s3_key` starts with `{organizationId}/reservations/{reservationId}/`.
9. Confirm `filepath` is populated as `{bucket}/{organizationId}/reservations/{reservationId}`.
10. Delete one image.
11. Confirm delete uses `ConfirmModalComponent`, not native browser dialogs.
12. Confirm S3/storage object is gone.
13. Confirm DB row is gone.
14. Confirm another organization cannot access/delete image.
