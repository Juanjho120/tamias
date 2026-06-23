# 15B — Organization Administration Page

## Status

Implemented / refined.

## Goal

Add organization administration with explicit `SUPER_ADMIN` ownership of global administration while keeping regular organization administrators scoped to their current organization.

## Role behavior

### SUPER_ADMIN

- Can access the Organizations page.
- Can create and edit organizations.
- Can activate/deactivate organizations.
- Can upload, replace, and delete organization logos.
- Can assign the `SUPER_ADMIN` role from the user creation/edit modal.
- Can manage existing users with the `SUPER_ADMIN` role.

### ADMINISTRATOR

- Cannot access the Organizations page.
- Cannot see the Organizations item in the sidebar.
- Cannot assign the `SUPER_ADMIN` role.
- Cannot see `SUPER_ADMIN` as an assignable role in the user creation/edit modal.
- Cannot manage users whose current role is `SUPER_ADMIN`.

## Backend hardening

`UserService` validates `SUPER_ADMIN` role assignment server-side. This prevents a regular `ADMINISTRATOR` from crafting a manual request to assign `SUPER_ADMIN` even if the frontend hides the option.

`UserService` also prevents regular `ADMINISTRATOR` users from editing/deleting users whose current role is `SUPER_ADMIN`.

## Frontend behavior

The user form modal receives `canAssignSuperAdmin` from the Users page.

- When the current user is `SUPER_ADMIN`, the role selector includes `SUPER_ADMIN`.
- When the current user is not `SUPER_ADMIN`, the role selector excludes `SUPER_ADMIN`.

The Users page also hides edit/delete/status actions for `SUPER_ADMIN` target users when the current user is not `SUPER_ADMIN`.

## Translation notes

Translations must remain in:

- `frontend/public/assets/i18n/es.json`
- `frontend/public/assets/i18n/en.json`

Do not add feature-specific `.ts` translation files.
