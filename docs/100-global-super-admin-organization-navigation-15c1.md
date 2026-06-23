# 15C.1 — Global SUPER_ADMIN Organization Navigation

## Status

Implemented.

## Goal

Allow a user who has at least one active `SUPER_ADMIN` membership to navigate across every active organization in TAMIAS without requiring an explicit `user_organizations` membership in each organization.

This keeps `SUPER_ADMIN` as a platform-level role and avoids losing elevated access when switching to an organization where the user either has no membership or has a lower organization-level role.

## Backend behavior

### Global SUPER_ADMIN detection

A user is treated as a global `SUPER_ADMIN` when they have at least one usable active membership with role `SUPER_ADMIN`.

A usable membership means:

- `user_organizations.status = ACTIVE`
- related organization is not deleted
- related organization status is `ACTIVE`

### Available organizations

`GET /api/v1/auth/organizations`

For normal users, the endpoint keeps the 15C behavior:

- returns only active organizations where the user has an active usable membership
- returns the role assigned in that membership

For global `SUPER_ADMIN` users, the endpoint now returns:

- every active non-deleted organization
- role `SUPER_ADMIN` for every organization option
- the current organization flag based on the JWT organization id

### Switching organization

`POST /api/v1/auth/switch-organization`

For normal users, the endpoint keeps the 15C behavior:

- requires an active usable membership in the target organization
- issues a token with the membership role

For global `SUPER_ADMIN` users, the endpoint now:

- validates only that the target organization is active and not deleted
- does not require a `user_organizations` row for the target organization
- issues a token with:
  - selected organization id
  - role `SUPER_ADMIN`

### Current user session

`GET /api/v1/auth/me`

For global `SUPER_ADMIN` users, `/me` now respects the organization id from the JWT and returns that organization with role `SUPER_ADMIN`.

For normal users, `/me` still validates the active membership for the organization id in the JWT.

### Login default organization

When a user has multiple active memberships, login prefers a usable `SUPER_ADMIN` membership first. If there is no `SUPER_ADMIN` membership, it falls back to the first usable active membership sorted by organization name.

## Security rules

- Do not use client-side organization overrides.
- Do not trust localStorage organization ids.
- Organization switching must always issue a new server-validated token.
- Global `SUPER_ADMIN` access is derived from server-side memberships, not frontend state.
- Inactive/deleted organizations are not available for switching.
- Normal users still require explicit active membership in the target organization.

## Non-goals

- Do not implement user organization membership management in this subphase.
- Do not add UI to assign users to organizations yet.
- Do not allow normal organization administrators to assign memberships outside their current organization.

Membership management is deferred to:

```text
15C.2 — User organization memberships management
```

## Verification checklist

- User with one normal organization sees only that organization.
- User with multiple normal memberships sees only those active memberships.
- User with `SUPER_ADMIN` membership sees all active organizations.
- `SUPER_ADMIN` can switch to an active organization without an explicit membership there.
- The switched token keeps role `SUPER_ADMIN`.
- `/api/v1/auth/me` returns the selected organization from the token.
- Normal users cannot switch to organizations where they do not have active membership.
- Inactive/deleted organizations are not listed or accepted as switch targets.
