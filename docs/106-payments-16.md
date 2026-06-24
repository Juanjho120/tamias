# 16 — Payments

## Status

Planned / next.

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
created_at
updated_at
deleted_at
```

Rules:

- Organization-scoped.
- Must follow the existing catalog table pattern.
- `status` should support the same catalog lifecycle used by current base catalogs.
- Soft delete through `deleted_at` and/or catalog status pattern already used in the project.
- Unique category names should be enforced per organization among non-deleted rows.

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
name
description
method
category_id
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
- `category_id` references `payment_categories`.
- `method` allowed values:
  - `CREDIT`
  - `DEBIT`
  - `CASH`
  - `BANK_TRANSFER`
- `amount` must be non-negative.
- `responsible` is a free-text field, not a user FK, so real-world names can be recorded even when the person is not a TAMIAS user.
- Use soft delete for payments.
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
- On replacement/removal, delete the S3 object and hard-delete the database row.
- Do not use soft delete for `payment_images`.
- Validate allowed content types consistently with other image modules:
  - JPG/JPEG
  - PNG
  - WEBP

## Expected Flyway

The current migration sequence should be checked before implementation. At the time this document was created, the next expected Flyway was:

```text
V40__create_payments.sql
```

Implementation must verify the latest migration number again before creating the Flyway.

## Backend scope

Suggested package:

```text
backend/src/main/java/com/tamias/payment
```

Suggested subpackages:

```text
controller
dto
entity
enums
mapper
repository
service
```

Suggested enum:

```text
PaymentMethod
- CREDIT
- DEBIT
- CASH
- BANK_TRANSFER
```

Suggested endpoints:

```http
GET    /api/v1/payments
GET    /api/v1/payments/{id}
POST   /api/v1/payments
PUT    /api/v1/payments/{id}
DELETE /api/v1/payments/{id}
```

Suggested filters for `GET /api/v1/payments`:

```text
propertyId
categoryId
method
dateFrom
dateTo
search
```

Rules:

- All reads/writes must be scoped to `CurrentUserService.getCurrentOrganizationId()`.
- `SUPER_ADMIN` should operate against the currently selected organization from the token, not globally across all organizations at once.
- Soft delete must prevent deleted payments from appearing in normal queries.
- Deleting a payment should also delete related payment images from S3 and DB, unless a safer existing project pattern says otherwise.

## Payment images backend scope

Suggested endpoints:

```http
GET    /api/v1/payments/{paymentId}/images
POST   /api/v1/payments/{paymentId}/images
DELETE /api/v1/payments/{paymentId}/images/{imageId}
```

Rules:

- Reuse existing S3 upload/presigned URL services and validation patterns.
- Do not create a duplicate S3 abstraction if an existing one already covers this use case.
- Return image metadata plus presigned URL like other entity image modules.
- Hard delete S3 object and DB row on delete.

## Catalog integration scope

Payment categories must become part of the existing catalog administration experience.

Suggested package:

```text
backend/src/main/java/com/tamias/catalog/paymentcategory
```

Frontend must add the payment category catalog tab/section following existing catalog UI patterns.

Rules:

- Do not create a separate payment category screen if current catalogs are centrally managed.
- Use existing catalog status/soft delete behavior.
- Use existing translation JSON files:
  - `frontend/public/assets/i18n/es.json`
  - `frontend/public/assets/i18n/en.json`

## Frontend scope

Suggested feature package:

```text
frontend/src/app/features/payments
```

Suggested UI:

- Payments page accessible from sidebar.
- Table with filters.
- Create/edit modal.
- Delete modal, not native `confirm()`.
- Images modal using the same UX pattern as other image modules.
- Total amount summary for the active filters.
- Method/category/property display labels.
- Strict Angular typing.
- Standalone Angular 19 pattern.
- ngx-translate keys in existing JSON files only.

Suggested columns:

```text
Pay date
Name
Category
Method
Amount
Property
Responsible
Actions
```

Suggested actions:

```text
Edit
Delete
Images
```

## AI scope

Add read-only TAMI tools for payments. No write actions.

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

## Proposed subphases

```text
16A Payments backend foundation
16B Payment images with S3 hard delete
16C Payment categories in catalogs
16D Angular payments page
16E AI awareness for payments
```

### 16A — Payments backend foundation

- Create Flyway.
- Create entities/enums/DTOs/repositories/services/controllers.
- Implement CRUD.
- Implement soft delete.
- Implement organization scoping.
- Implement filters.
- Do not add images yet unless needed by the same Flyway.

### 16B — Payment images with S3 hard delete

- Add payment image entity/repository/service/controller.
- Upload images to S3.
- Generate presigned URLs.
- Delete S3 object and database row on removal.
- Validate content types and size.

### 16C — Payment categories in catalogs

- Add payment category backend catalog support.
- Add payment category to frontend catalog administration.
- Add translation keys.

### 16D — Angular payments page

- Add payments feature page.
- Add sidebar item.
- Add list/filter/create/edit/delete UX.
- Add images modal.
- Add translation keys.

### 16E — AI awareness for payments

- Add payment read-only repository/service/handler.
- Add routing examples/patterns.
- Add smoke-test prompts.
- Keep AI tools read-only.

## Non-goals

- Do not process real payments.
- Do not store card numbers, CVV, bank account numbers or payment credentials.
- Do not add Reports implementation in this phase.
- Do not add Notifications/reminders in this phase.
- Do not add Blueprint Analysis in this phase.
- Do not add TAMI write actions.

## Relationship with Reports

Payments should be implemented before Reports because operational reports will likely need payment data for expense summaries, category totals, monthly totals and property-level financial views.
