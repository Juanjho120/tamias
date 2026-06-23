# 15C.2 — User Organization Memberships Management

## Status

Implemented.

## Goal

Allow only `SUPER_ADMIN` users to assign users to additional organizations and define the role each user has in each organization.

This completes the operational side of the organization switcher: regular users can switch only between explicit active memberships, while `SUPER_ADMIN` users can manage those memberships for other users.

## Backend endpoints

All endpoints are under the existing Users backend area and are protected with `SUPER_ADMIN` only:

```http
GET /api/v1/users/{userId}/organizations
POST /api/v1/users/{userId}/organizations
PUT /api/v1/users/{userId}/organizations/{organizationId}
DELETE /api/v1/users/{userId}/organizations/{organizationId}
```

## Backend behavior

### List memberships

Returns non-deleted memberships for the target user, including:

- organization id
- organization name
- organization logo URL
- role
- membership status
- created/updated timestamps

### Assign membership

Creates or reactivates a membership for the target user in the selected active organization.

Request:

```json
{
  "organizationId": "...",
  "role": "PROPERTY_MANAGER"
}
```

### Update membership

Updates role and status for an existing membership.

Request:

```json
{
  "role": "ADMINISTRATOR",
  "status": "ACTIVE"
}
```

`DELETED` is not accepted in the update endpoint. Use the delete endpoint for deletion.

### Delete membership

Soft-deletes the membership by setting `user_organizations.status = DELETED`.

## Rules

- Only `SUPER_ADMIN` can manage multi-organization memberships.
- `ADMINISTRATOR` cannot see or use these controls.
- `SUPER_ADMIN` can assign any role, including `SUPER_ADMIN`.
- The backend prevents removing a user's last active usable `SUPER_ADMIN` membership.
- Normal user creation/update remains scoped to the active organization.
- Organization switcher behavior remains token-based and does not use a client-side organization override.

## Frontend behavior

The Users page shows a memberships action only for `SUPER_ADMIN` users.

The memberships modal allows a `SUPER_ADMIN` to:

- view a user's organization memberships
- assign a user to another active organization
- choose the role for the target organization
- update membership role/status
- remove a membership

The modal reuses existing snackbar and modal patterns and does not use native `confirm()`.

## Translation keys

This phase adds `userOrganizations.*` keys to `es.json` and `en.json`.

## Verification checklist

- `SUPER_ADMIN` can open the memberships modal from the Users page.
- `ADMINISTRATOR` cannot see the memberships action.
- `SUPER_ADMIN` can assign a user to another organization.
- `SUPER_ADMIN` can choose any role, including `SUPER_ADMIN`.
- Membership changes affect what normal users can see in the organization switcher.
- The backend blocks removing the last usable `SUPER_ADMIN` membership for a user.
- No frontend text is hardcoded outside i18n keys.
