# 15C — Organization switcher / multi-organization navigation

## Status

Implemented and refined.

## Goal

Allow users with multiple active organizations to switch the active organization from the UI while keeping the backend as the source of truth for authorization and tenant context.

## Runtime behavior

The frontend does not store an organization override. It requests a backend-validated organization switch and receives a new JWT/session response.

The active organization used by organization-scoped endpoints comes from the JWT `organizationId` claim through `CurrentUserService`.

## Refinement

The switcher must:

- render the currently active organization as the selected option;
- remain disabled while the switch request is in flight;
- restore the current organization if the switch fails;
- reload available organization options after a successful switch;
- navigate to the dashboard after switching to clear organization-scoped page state.

The JWT authentication filter must preserve the `organizationId` and `role` from the validated JWT. It can still load the user from the database to validate that the user exists and matches the token subject, but it must not replace the selected organization with the user's default database membership.

## Backend endpoints

```http
GET /api/v1/auth/organizations
POST /api/v1/auth/switch-organization
```

## Security rules

Regular users can switch only to active organizations where they have active membership.

Global `SUPER_ADMIN` users can switch to any active organization and keep `SUPER_ADMIN` as their token role.

## Verification checklist

- The switcher defaults to the organization from the current session.
- The switcher is disabled while switching organization.
- After a successful switch, the stored token changes.
- After a successful switch, `/api/v1/auth/me` returns the selected organization.
- Organization-scoped screens load data from the selected organization.
- Failed switches restore the previous selected organization in the UI.
