# 15A — Organization Logo Backend + Current Organization Header

## Status

Implemented.

## Goal

Add logo support to the current organization and display the selected organization's logo next to its name in the application header.

This phase is intentionally limited to the current organization experience. Full organization administration and cross-organization navigation are handled in later subphases:

- 15B — Organization administration page
- 15C — Organization switcher / multi-organization navigation

## Backend scope

### Database

Adds logo metadata to `organizations` using Flyway migration:

```text
V37__add_organization_logo_metadata.sql
```

Columns:

```text
logo_original_filename
logo_s3_key
logo_filepath
logo_content_type
logo_size_bytes
logo_updated_at
```

The logo file is stored in the existing private storage/S3 flow. The database stores metadata only; binary content is not stored in PostgreSQL.

### Entity and DTOs

`Organization` now stores logo metadata.

`OrganizationResponse` now includes:

```text
logoUrl
logoOriginalFilename
logoContentType
logoSizeBytes
logoUpdatedAt
```

`AuthOrganizationResponse` now includes:

```text
logoUrl
```

This allows `/api/v1/auth/login` and `/api/v1/auth/me` to hydrate the frontend session with the selected organization's logo URL.

### Endpoints

Current organization endpoints:

```http
GET /api/v1/organizations/current
PUT /api/v1/organizations/current
POST /api/v1/organizations/current/logo
PUT /api/v1/organizations/current/logo
DELETE /api/v1/organizations/current/logo
```

Logo upload accepts multipart form data:

```text
file=<image file>
```

Allowed content types reuse `ImageValidationService`:

```text
image/jpeg
image/png
image/webp
```

Upload/delete are restricted to users with `ADMINISTRATOR` role for the current organization.

## Frontend scope

The main layout header now displays:

```text
[organization logo] user name
                    organization name
```

If the organization does not have a logo, the header shows a small initials fallback based on the organization name.

The frontend auth model was updated so `AuthOrganization.logoUrl` is available from the stored session.

## Out of scope

This phase does not include:

- SUPER_ADMIN global organization management.
- Organization list page.
- Organization switcher.
- Organization CRUD for all organizations.
- UI form to upload the logo from an administration page.

Those belong to 15B and 15C.

## Verification checklist

Backend:

```bash
cd backend
./mvnw test
```

Manual API checks:

```http
GET /api/v1/organizations/current
POST /api/v1/organizations/current/logo
DELETE /api/v1/organizations/current/logo
GET /api/v1/auth/me
```

Expected behavior:

- `GET /api/v1/organizations/current` returns logo metadata and `logoUrl` when a logo exists.
- `GET /api/v1/auth/me` returns `user.organization.logoUrl`.
- Replacing a logo deletes the previous storage object.
- Deleting a logo clears metadata and deletes the storage object.

Frontend:

```bash
cd frontend
npm start
```

Expected behavior:

- The current organization logo appears next to the selected organization name in the header.
- If no logo exists, the initials fallback appears.
- Existing login/session behavior remains compatible with users stored before this field existed.
