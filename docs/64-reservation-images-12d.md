# 12D — Reservation Images

## Purpose

Allow multiple images to be attached to a reservation.

Examples:

```text
check-in evidence
special guest setup
reservation-related damages
parking/access evidence
before/after photos
```

---

## Dependency

This phase depends on:

```text
11A — S3 key strategy + filepath fields
11B — Hard delete policy for entity images
```

---

## Database

Create:

```text
reservation_images
```

Required logical fields:

```text
id
reservation_id
s3_key
filepath
filename/original_filename
content_type
size_bytes
created_at
created_by
```

Do not add soft-delete columns.

Before implementing, inspect the actual reservation entity/table and existing image entity patterns.

---

## S3 path

Use:

```text
{organizationId}/reservations/{reservationId}/{filename}
```

Example:

```text
s3_key: reservations/73d8f9d3-4f6a-4e4a-86be-d89a9b58b98e/checkin.jpg
filepath: tamias-dev-files/reservations/73d8f9d3-4f6a-4e4a-86be-d89a9b58b98e
```

Bucket must come from configuration.

---

## Backend API

Recommended endpoint pattern:

```http
GET    /api/v1/reservations/{reservationId}/images
POST   /api/v1/reservations/{reservationId}/images
DELETE /api/v1/reservations/{reservationId}/images/{imageId}
```

Rules:

- Parent reservation must belong to current organization.
- Delete must remove S3 object and DB row physically.
- Do not allow image access across organizations.

---

## Frontend

Reservations screen should include an images action.

UI pattern:

```text
Reservations table
  Images button
    opens modal
      list existing images
      upload one or more images
      delete image
```

Use the same pattern as properties and maintenance images.

---

## AI impact

Future AI file/image tools should be able to answer questions such as:

```text
¿Qué reservaciones tienen imágenes?
¿Qué imágenes tiene la reservación BOOK0098798?
```

Not required unless file/image AI tools are updated in the same phase.

---

## Acceptance tests

```text
1. Open Reservations screen.
2. Click Images for a reservation.
3. Upload several images.
4. Confirm images appear in modal.
5. Confirm s3_key starts with reservations/{reservationId}/.
6. Confirm filepath is populated.
7. Delete one image.
8. Confirm S3 object is gone.
9. Confirm DB row is gone.
10. Confirm another organization cannot access/delete image.
```
