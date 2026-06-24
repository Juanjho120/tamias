# 16 — Payments

## Status

In progress through 16B.

## Purpose

Add a small organization-scoped payment module to TAMIAS for registering operational payments and expenses.

This module is not a payment processor. It does not charge cards, store card numbers, store bank account numbers or execute transfers. It is an internal operational registry for payments already made or to be tracked manually.

## Goals

- Register payments with method, category, amount, responsible person and pay date.
- Associate payments with the active organization.
- Optionally associate payments with a property.
- Manage payment categories as part of TAMIAS catalogs.
- Attach receipt/payment images using the existing private S3 + presigned URL pattern.
- Hard-delete payment images from both S3 and database when removed.
- Soft-delete payment records.
- Add a dedicated Angular Payments screen.
- Add read-only TAMI awareness for payment search and summaries.

## Database design

### `payment_categories`

Catalog table for payment categories.

Fields:

```text
id
organization_id
name
description
status
created_by
updated_by
created_at
updated_at
deleted_at
deleted_by
```

Rules:

- Organization-scoped.
- Follows the current `BaseCatalogEntity` pattern.
- `status` supports the current catalog lifecycle: `ACTIVE`, `INACTIVE`, `DELETED`.
- Soft delete through `deleted_at` and catalog status.
- Unique category names are enforced per organization among rows following the current catalog pattern.

Examples:

```text
Electricidad
Agua
Internet
Limpieza
Reparaciones
Jardinería
Impuestos
Comisiones
Servicios externos
```

### `payments`

Main payment registry.

Fields:

```text
id
organization_id
property_id nullable
category_id
name
description
method
amount
responsible
pay_date
status
created_by
updated_by
deleted_by
created_at
updated_at
deleted_at
```

Rules:

- Organization-scoped.
- `property_id` is optional so payments can be global to the organization or linked to a property.
- `category_id` references `payment_categories` and is required.
- `method` allowed values:
  - `CREDIT`
  - `DEBIT`
  - `CASH`
  - `BANK_TRANSFER`
- `amount` must be non-negative.
- `responsible` is a free-text field, not a user FK, so real-world names can be recorded even when the person is not a TAMIAS user.
- Payments use soft delete through `status = DELETED` and `deleted_at`.
- Do not store sensitive card/bank details.

### `payment_images`

Payment receipt/image table.

Fields:

```text
id
organization_id
payment_id
original_filename
s3_key
filepath
content_type
size_bytes
status
created_by
created_at
```

Rules:

- Organization-scoped.
- `payment_id` references `payments`.
- Image content is stored in S3, never in `bytea`.
- Use private S3 objects with presigned URLs.
- Follow the existing S3 metadata pattern used by other image modules.
- On removal, delete the S3 object and hard-delete the database row.
- Do not use soft delete for `payment_images`.
- Validate allowed content types consistently with other image modules:
  - JPG/JPEG
  - PNG
  - WEBP

## 16A — Payments backend foundation

Status: Implemented.

Implemented scope:

- `V40__create_payments.sql`.
- `payment_categories` table.
- `payments` table.
- `payment_images` table prepared for 16B.
- `PaymentMethod` enum.
- `PaymentStatus` enum.
- `Payment` entity.
- `PaymentImage` entity/repository prepared for 16B.
- `PaymentCategory` catalog entity/repository/service/controller.
- `PaymentRequest` and `PaymentResponse` DTOs.
- `PaymentMapper`.
- `PaymentRepository` with organization-scoped filters.
- `PaymentService` with CRUD, organization scoping, date range validation and soft delete.
- `PaymentController` under `/api/v1/payments`.

Endpoints added:

```http
GET    /api/v1/payments
GET    /api/v1/payments/{id}
POST   /api/v1/payments
PUT    /api/v1/payments/{id}
DELETE /api/v1/payments/{id}

GET    /api/v1/catalogs/payment-categories
GET    /api/v1/catalogs/payment-categories/{id}
POST   /api/v1/catalogs/payment-categories
PUT    /api/v1/catalogs/payment-categories/{id}
DELETE /api/v1/catalogs/payment-categories/{id}
```

Filters for `GET /api/v1/payments`:

```text
propertyId
categoryId
method
dateFrom
dateTo
search
```

Security rules:

- Reads are allowed for `ADMINISTRATOR`, `PROPERTY_MANAGER`, `MAINTENANCE_STAFF` and `READ_ONLY`.
- Create/update are allowed for `ADMINISTRATOR`, `PROPERTY_MANAGER` and `MAINTENANCE_STAFF`.
- Delete is allowed for `ADMINISTRATOR` and `PROPERTY_MANAGER`.
- `SUPER_ADMIN` works through inherited authorities and the selected organization in the token.

Non-scope for 16A:

- No payment image S3 upload/delete endpoints yet.
- No frontend Payments page yet.
- No catalog frontend integration yet.
- No TAMI payment tools yet.
- No Reports integration yet.

## 16B — Payment images with S3 hard delete

Status: Implemented.

Implemented scope:

- `PaymentImageController` under `/api/v1/payments/{paymentId}/images`.
- `PaymentImageService` with organization-scoped reads, upload, file serving and deletion.
- S3 upload through the existing `FileStorageService` pattern.
- Presigned image URLs through the existing `ImageMapper` / `FileStorageService.buildFileUrl(...)` pattern.
- Image validation through the existing `ImageValidationService`.
- Hard delete on image removal: delete S3 object and physically delete the `payment_images` row.
- `ImageMapper` now maps `PaymentImage` to `ImageResponse` and `ImageUploadResponse`.

Endpoints added:

```http
GET    /api/v1/payments/{paymentId}/images
GET    /api/v1/payments/{paymentId}/images/{imageId}
POST   /api/v1/payments/{paymentId}/images
GET    /api/v1/payments/{paymentId}/images/{imageId}/file
DELETE /api/v1/payments/{paymentId}/images/{imageId}
```

Security rules:

- Reads and file preview are allowed for `ADMINISTRATOR`, `PROPERTY_MANAGER`, `MAINTENANCE_STAFF` and `READ_ONLY`.
- Upload/delete are allowed for `ADMINISTRATOR`, `PROPERTY_MANAGER` and `MAINTENANCE_STAFF`.
- `SUPER_ADMIN` works through inherited authorities and the selected organization in the token.

Non-scope for 16B:

- No frontend Payments page yet.
- No catalog frontend integration yet.
- No TAMI payment tools yet.
- No Reports integration yet.

## 16C — Payment categories in catalogs

Planned.

- Add payment category to frontend catalog administration.
- Add translation keys.

Backend payment-category endpoints were added in 16A so the frontend can consume them in 16C.

## 16D — Angular payments page

Planned.

- Add payments feature page.
- Add sidebar item.
- Add list/filter/create/edit/delete UX.
- Add images modal.
- Add translation keys.

## 16E — AI awareness for payments

Planned.

- Add payment read-only repository/service/handler.
- Add routing examples/patterns.
- Add smoke-test prompts.
- Keep AI tools read-only.

Suggested tools:

```text
payment.summary
payment.search
payment.recent
payment.byCategory
payment.byMethod
payment.byProperty
payment.monthlyTotals
payment.highestPayments
payment.imagesSummary
```

Questions TAMI should handle:

```text
¿Cuánto pagué este mes?
¿Cuáles fueron los últimos pagos?
¿Cuánto he pagado por electricidad?
¿Qué pagos hice en efectivo?
¿Qué pagos tienen imágenes?
¿Cuál fue el pago más alto?
¿Cuánto se pagó por la Casa A?
¿Qué pagos hay sin categoría?
```

Rules:

- AI tools must be organization-scoped.
- AI tools must respect the selected organization for `SUPER_ADMIN`.
- AI tools must be read-only.
- AI tools must not expose raw S3 keys unless the existing image tools already do so.
- Avoid mixing payment answers with purchase-list answers unless the user's question clearly asks for both.

## Non-goals

- Do not process real payments.
- Do not store card numbers, CVV, bank account numbers or payment credentials.
- Do not add Reports implementation in this phase.
- Do not add Notifications/reminders in this phase.
- Do not add Blueprint Analysis in this phase.
- Do not add TAMI write actions.

## Relationship with Reports

Payments should be implemented before Reports because operational reports will likely need payment data for expense summaries, category totals, monthly totals and property-level financial views.
