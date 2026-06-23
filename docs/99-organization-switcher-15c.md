# 15C — Organization Switcher / Multi-Organization Navigation

## Status

Implemented.

## Goal

Allow authenticated users who belong to more than one active organization to switch the active organization from the TAMIAS UI.

The active organization is not stored as a client-only override. The backend validates the membership and returns a new JWT/session context for the selected organization.

## Backend scope

### New DTOs

- `AuthOrganizationOptionResponse`
- `SwitchOrganizationRequest`

### New endpoints

```http
GET /api/v1/auth/organizations
```

Returns active organizations available to the authenticated user.

```json
[
  {
    "id": "...",
    "name": "Organization A",
    "role": "ADMINISTRATOR",
    "logoUrl": "...",
    "current": true
  }
]
```

```http
POST /api/v1/auth/switch-organization
```

Request:

```json
{
  "organizationId": "..."
}
```

Response:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "...",
    "firstName": "...",
    "lastName": "...",
    "email": "...",
    "role": "ADMINISTRATOR",
    "organization": {
      "id": "...",
      "name": "Organization B",
      "logoUrl": "..."
    },
    "passwordChangeRequired": false
  }
}
```

## Validation rules

The switch endpoint validates that:

- the authenticated user exists and is active
- the target organization exists through an active `user_organizations` membership
- the membership status is `ACTIVE`
- the target organization status is `ACTIVE`
- the target organization is not soft-deleted

If any of these checks fail, the backend returns a controlled error and no token is issued.

## Session behavior

- Login still selects a default active organization.
- `/api/v1/auth/me` now respects the organization id from the current JWT instead of falling back to the first active membership.
- Switching organizations issues a new JWT with the selected `organizationId` and the role assigned for that membership.
- The frontend replaces the stored token and current user session with the switch response.

## Frontend scope

- `AuthService` can list available organizations.
- `AuthService` can switch the active organization and persist the new session.
- `MainLayoutComponent` shows an organization selector when more than one active organization is available.
- The active organization logo continues to be shown next to the organization selector/name.
- After switching, the UI navigates to `/dashboard` to clear organization-scoped screen state.

## i18n keys

The implementation expects these keys in `es.json` and `en.json`:

```json
"organizationSwitcher": {
  "label": "...",
  "messages": {
    "loadError": "...",
    "switched": "...",
    "switchError": "..."
  }
}
```

## Non-goals

- Do not allow arbitrary organization id overrides from localStorage or query params.
- Do not change tenant boundaries in existing services.
- Do not let users switch to organizations where they do not have an active membership.
- Do not implement global impersonation for `SUPER_ADMIN` in this subphase.

## Verification checklist

- A user with one active organization sees only the organization name.
- A user with multiple active organizations sees the selector.
- Switching organizations returns a new JWT.
- Local storage token and user session are replaced after switching.
- `/api/v1/auth/me` reflects the organization from the active JWT.
- Organization-scoped pages load data for the selected organization after switching.
- Inactive/deleted memberships and inactive/deleted organizations are not listed.
