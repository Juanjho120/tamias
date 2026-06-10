# TAMIAS — Multi-Tenant Review

TAMIAS uses:

```text
Shared database + shared schema + organization_id
```

This document lists the expected review rules for organization isolation.

---

## 1. Core rule

The backend must resolve the active organization from the authenticated user context.

Do not trust:

```text
organizationId from frontend request body
organizationId from query params
organizationId from route params
```

---

## 2. Modules to review

### Users Management

- [ ] `/api/v1/users` is Administrator only.
- [ ] Users list is filtered by current organization.
- [ ] Admin cannot retrieve users from another organization.
- [ ] Admin cannot update users from another organization.
- [ ] Admin cannot delete users from another organization.
- [ ] Admin cannot delete own user.

### Profile

- [ ] `/api/v1/profile` uses current user id from authentication.
- [ ] User cannot edit another user's profile.
- [ ] Profile cannot change role.
- [ ] Profile cannot change status.
- [ ] Profile cannot change organization.
- [ ] Password change validates current password.

### Properties

- [ ] findAll filters by organization.
- [ ] findById filters by organization.
- [ ] create sets organization from current user.
- [ ] update validates organization ownership.
- [ ] delete validates organization ownership.

### Inventory Items

- [ ] findAll filters by organization.
- [ ] findById filters by organization.
- [ ] create sets organization from current user.
- [ ] update validates organization ownership.
- [ ] delete validates organization ownership.

### Maintenance, reservations, purchases, documents and AI

- [ ] Maintenance records filter by organization.
- [ ] Maintenance detail items filter by organization.
- [ ] Scheduled maintenance filters by organization.
- [ ] Reservations filter by organization.
- [ ] Reservation supplies belong to same organization.
- [ ] Purchase lists filter by organization.
- [ ] Purchase items belong to same organization.
- [ ] Documents filter by organization.
- [ ] Download URLs validate organization ownership.
- [ ] AI chat sessions filter by organization.
- [ ] Document search filters by organization.

---

## 3. Manual attack test

Create two organizations:

```text
Organization A
Organization B
```

Then:

1. Login as user from Organization A.
2. Copy an ID from Organization B.
3. Try to access it directly through the API.
4. Expected result:

```text
404 Not Found
```

or:

```text
403 Forbidden
```

Never:

```text
200 OK
```
