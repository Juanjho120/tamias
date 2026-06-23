# 15C Fix — Organization Switcher Last Selection Persistence

## Status

Implemented.

## Goal

Make the organization switcher persist and restore the last organization selected by each user.

This avoids login/session mismatches where the backend loads data for one organization while the selector visually shows the first organization in the dropdown.

## Backend behavior

A nullable `last_organization_id` column is added to `users`.

When a user switches organization through:

```http
POST /api/v1/auth/switch-organization
```

TAMIAS stores the selected organization as the user's last organization.

On the next login:

- `SUPER_ADMIN` users are returned to their last active organization, even if they do not have an explicit membership in that organization.
- Regular users are returned to their last active organization only if they still have an active membership there.
- If the last organization is no longer available, TAMIAS falls back to the default active membership.

## SUPER_ADMIN behavior

`SUPER_ADMIN` remains global.

The selected organization controls data scoping, but the role in the JWT remains `SUPER_ADMIN`.

## Frontend behavior

The selector is explicitly bound to the current organization id from the authenticated session.

The active option is also marked with `[selected]` to avoid the browser visually falling back to the first option while async organization options are loading.

The switcher remains disabled while a switch request is in progress.

## Verification

1. Log in as a `SUPER_ADMIN` created in organization B.
2. Confirm the current organization logo/data belongs to organization B.
3. Confirm the switcher shows organization B selected, not the first alphabetical option.
4. Switch to organization A.
5. Log out.
6. Log in again.
7. Confirm organization A is loaded and selected by default.
8. Repeat the same flow with a non-super-admin user that belongs to more than one organization.
