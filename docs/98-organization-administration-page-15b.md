# 15B — Organization Administration Page

## Status

Implemented.

## Goal

Add an organization administration page with different behavior for platform `SUPER_ADMIN` users and organization `ADMINISTRATOR` users.

## Role model

### SUPER_ADMIN

`SUPER_ADMIN` is a platform-level role.

It can:

- View all organizations.
- Create organizations.
- Edit any organization.
- Activate or deactivate organizations.
- Upload, replace or delete the logo of any organization.

### ADMINISTRATOR

`ADMINISTRATOR` remains organization-scoped.

It can:

- View only the current organization.
- Edit only the current organization.
- Upload, replace or delete only the current organization logo.

It cannot:

- View other organizations.
- Create organizations.
- Activate or deactivate organizations.
- Assign `SUPER_ADMIN` to users.

## Backend changes

### Role seed

A new Flyway migration seeds the platform role:

```text
V38__seed_super_admin_role.sql
```

It inserts or updates:

```text
SUPER_ADMIN
```

### Organization endpoints

The existing `/api/v1/organizations/current` endpoints remain available.

The administration page uses these endpoints:

```http
GET /api/v1/organizations
GET /api/v1/organizations/{id}
POST /api/v1/organizations
PUT /api/v1/organizations/{id}
PATCH /api/v1/organizations/{id}/status
POST /api/v1/organizations/{id}/logo
PUT /api/v1/organizations/{id}/logo
DELETE /api/v1/organizations/{id}/logo
```

Authorization behavior:

- `SUPER_ADMIN` can manage all organizations.
- `ADMINISTRATOR` can only access and update its current organization.
- `POST /api/v1/organizations` is `SUPER_ADMIN` only.
- `PATCH /api/v1/organizations/{id}/status` is `SUPER_ADMIN` only.

### User role hardening

`RoleCode` now includes `SUPER_ADMIN`, but organization administrators cannot assign that role through the users API.

`UserService` validates that only an authenticated `SUPER_ADMIN` can assign the `SUPER_ADMIN` role.

## Frontend changes

Adds:

```text
/organizations
```

The page shows:

- A table/list of organizations.
- Organization logo preview or initials fallback.
- Create organization action for `SUPER_ADMIN`.
- Edit organization action for `SUPER_ADMIN` and current organization `ADMINISTRATOR`.
- Upload/replace/delete logo actions.
- Activate/deactivate actions for `SUPER_ADMIN`.

The main layout exposes the Organizations entry for:

- `SUPER_ADMIN`
- `ADMINISTRATOR`

## Non-goals

This phase does not implement organization switching. That belongs to:

```text
15C — Organization switcher / multi-organization navigation
```

This phase does not migrate global action buttons to icon-only. That belongs to:

```text
15D — Icon-only action buttons with tooltips
```

## Verification checklist

### Backend

- `SUPER_ADMIN` role exists after Flyway runs.
- `SUPER_ADMIN` can list all organizations.
- `SUPER_ADMIN` can create an organization.
- `SUPER_ADMIN` can update any organization.
- `SUPER_ADMIN` can activate/deactivate organizations.
- `SUPER_ADMIN` can upload/delete any organization logo.
- `ADMINISTRATOR` can list only the current organization.
- `ADMINISTRATOR` can edit only the current organization.
- `ADMINISTRATOR` can upload/delete only the current organization logo.
- `ADMINISTRATOR` cannot create organizations.
- `ADMINISTRATOR` cannot activate/deactivate organizations.
- `ADMINISTRATOR` cannot assign `SUPER_ADMIN` through user management.

### Frontend

- `/organizations` loads for `ADMINISTRATOR`.
- `/organizations` loads for `SUPER_ADMIN`.
- `ADMINISTRATOR` sees only current organization.
- `SUPER_ADMIN` sees all organizations.
- Logo upload refreshes the organization row.
- Updating the current organization's name/logo updates the header session state.
- No native `confirm()` is used.
