# TAMIAS — MVP Hardening

This document defines the MVP hardening phase for TAMIAS.

The goal is not to add major new product features. The goal is to make the current MVP safer, more stable, easier to validate, and easier to deploy.

---

## 1. Scope

This phase covers:

- CI validation.
- Clean local setup documentation.
- Environment variable documentation.
- Security review.
- Multi-tenant review.
- Manual QA checklist.
- Build validation.
- Error-handling review.
- Test coverage plan.
- First-login temporary password validation.
- User/profile access validation.

This phase should avoid large product behavior changes unless a bug or security gap is found.

---

## 2. Current MVP areas to protect

Critical flows:

```text
Authentication
Mandatory temporary password change
My Profile
Users Management
Organizations and users
Properties
Inventory Items
Maintenance Records
Maintenance Record Items
Scheduled Maintenance
Reservations
Reservation Supplies
Task Lists
Purchase Lists
Purchase Items
Documents
S3 file handling
Document processing
RAG search
AI chat sessions
Dashboard calendar
```

---

## 3. Hardening goals

### Stability

- Backend must build from a clean checkout.
- Frontend must build from a clean checkout.
- Flyway migrations must run on an empty PostgreSQL database.
- Docker Compose must start required local services.
- Core flows must work after restart.

### Security

- JWT validation must protect private endpoints.
- Role-based access must be enforced in backend.
- Organization isolation must be enforced in backend.
- `/users` must be administrator-only.
- `/profile` must be accessible to every authenticated user.
- New admin-created users must be forced to change temporary password.
- S3 files must remain private.
- Pre-signed URLs must be temporary.
- Secrets must not be committed.

---

## 4. Backend hardening checklist

### Authentication and authorization

- [ ] Unauthenticated requests return `401`.
- [ ] Unauthorized role access returns `403`.
- [ ] Admin-only endpoints are protected.
- [ ] `/api/v1/users` requires `ADMINISTRATOR`.
- [ ] `/api/v1/profile` works for every authenticated role.
- [ ] Read-only users cannot create/update/delete operational records.
- [ ] Maintenance staff cannot access admin-only resources.

### Mandatory temporary password change

- [ ] Admin-created users are created with `password_change_required = true`.
- [ ] Login response includes `passwordChangeRequired`.
- [ ] Password change endpoint sets `password_change_required = false`.
- [ ] Password change requires current password.
- [ ] Password confirmation must match.
- [ ] New password must be different from current password.

### Multi-tenant enforcement

For every operational endpoint, confirm the service layer filters by current organization:

- [ ] Properties.
- [ ] Catalogs.
- [ ] Inventory Items.
- [ ] Maintenance Records.
- [ ] Maintenance Record Items.
- [ ] Scheduled Maintenance.
- [ ] Reservations.
- [ ] Reservation Supplies.
- [ ] Task Lists.
- [ ] Purchase Lists.
- [ ] Documents.
- [ ] AI Chat Sessions.

### Database

- [ ] Flyway runs on empty PostgreSQL database.
- [ ] `ddl-auto=validate` passes.
- [ ] Important foreign keys exist.
- [ ] Important unique constraints exist.
- [ ] `users.password_change_required` exists.
- [ ] Soft delete filters are applied where expected.
- [ ] No migration already applied is edited.

---

## 5. Frontend hardening checklist

### Authentication

- [ ] Login works.
- [ ] Invalid credentials show a clear error.
- [ ] Token is attached to authenticated requests.
- [ ] Logout clears session data.
- [ ] Protected routes redirect to login when unauthenticated.
- [ ] Guest guard redirects authenticated users away from login.

### Mandatory password change

- [ ] New user login redirects to `/profile`.
- [ ] `passwordChangeGuard` is connected with `canActivateChild`.
- [ ] User cannot navigate to `/dashboard` while password change is required.
- [ ] After successful password change, session updates.
- [ ] After successful password change, user can access dashboard.
- [ ] Next login no longer forces password change.

### Profile

- [ ] Any authenticated user can open `/profile`.
- [ ] User can update first name.
- [ ] User can update last name.
- [ ] User can change password.
- [ ] Email cannot be changed from profile.
- [ ] Role cannot be changed from profile.

### Users Management

- [ ] Users menu appears only for Administrator.
- [ ] Non-admin cannot open `/users` successfully because backend rejects it.
- [ ] Admin can create user.
- [ ] Admin can update role/status.
- [ ] Admin can activate/deactivate user.
- [ ] Admin cannot deactivate/delete own user from UI.
- [ ] Backend still enforces rules even if UI is bypassed.

---

## 6. CI

The CI workflow validates:

```text
backend build
frontend build
```

Initial CI intentionally skips backend tests:

```text
-DskipTests
```

Reason:

- Current goal is to make CI catch compilation/build issues first.
- Service tests can be added in a later hardening sub-block.

Recommended future CI improvements:

```text
backend tests with test profile
frontend tests with headless browser
Docker image build
linting
dependency vulnerability scan
```

---

## 7. Definition of Done

The MVP hardening phase is considered ready when:

- `docker compose up -d` starts local dependencies.
- Backend starts locally.
- Frontend starts locally.
- Backend build passes.
- Frontend build passes.
- CI build passes on GitHub.
- Core smoke test passes.
- Mandatory password change flow works.
- No known security gap exists in basic role/multi-tenant flows.
- `.env.example` is present and safe.
- Documentation matches the current implementation.
