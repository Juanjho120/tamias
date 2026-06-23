# 15B — Organization Administration Page

## Status

Implemented / refined.

## Summary

This phase adds an organization administration page and introduces the `SUPER_ADMIN` role.

## Access rules

- `SUPER_ADMIN` can manage all organizations.
- `ADMINISTRATOR` can keep operational access, but the Organizations sidebar entry is only shown to `SUPER_ADMIN`.
- `SUPER_ADMIN` inherits operational authorities so existing endpoints protected with `ROLE_ADMINISTRATOR`, `ROLE_PROPERTY_MANAGER`, `ROLE_MAINTENANCE_STAFF`, or `ROLE_READ_ONLY` continue to work.

## Frontend notes

- Route added: `/organizations`.
- Sidebar item uses `navigation.organizations`.
- Organization page and modal use the existing JSON i18n files. No feature-specific translation `.ts` files should be introduced.
- Add `roles.SUPER_ADMIN.label` and `roles.SUPER_ADMIN.description` to `es.json` and `en.json`.

## Verification

- Login as `SUPER_ADMIN`.
- Confirm `/organizations` is reachable.
- Confirm the sidebar shows Organizations only for `SUPER_ADMIN`.
- Confirm existing pages do not show forbidden errors for `SUPER_ADMIN`.
- Confirm `roles.SUPER_ADMIN.label` no longer appears as a raw translation key in the Users page.
