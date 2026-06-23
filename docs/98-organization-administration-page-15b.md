# 15B — Organization Administration Page

## Status

Implemented / refined.

## Goal

Add an organization administration page while keeping the current multi-tenant security model intact.

## Role model

### SUPER_ADMIN

- Can list all organizations.
- Can create organizations.
- Can edit any organization.
- Can activate/deactivate organizations.
- Can upload, replace, or delete any organization logo.
- Receives inherited backend authorities so existing administrator-only operational pages remain available.

### ADMINISTRATOR

- Can keep using existing organization-scoped operational pages.
- Cannot access the global organization administration page from the sidebar.
- Cannot assign `SUPER_ADMIN` to other users.

## Frontend i18n rule

Organization page labels must use the existing JSON translation files:

- `frontend/public/assets/i18n/es.json`
- `frontend/public/assets/i18n/en.json`

Do not add feature-local TypeScript translation files for this module.

## Verification checklist

- Login as `SUPER_ADMIN`.
- Existing pages such as Dashboard, Properties, Catalogs, Maintenance, Purchases, Documents, Product Box Models, TAMI and Users remain accessible.
- Sidebar shows `Organizations` only for `SUPER_ADMIN`.
- Login as `ADMINISTRATOR`.
- Sidebar does not show `Organizations`.
- Organization page labels are translated through `es.json` / `en.json`.
