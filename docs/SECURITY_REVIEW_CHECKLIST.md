# TAMIAS — Security Review Checklist

This checklist is for reviewing security before deployment or public demo.

---

## 1. Authentication

- [ ] JWT secret is strong and not committed.
- [ ] Tokens expire.
- [ ] Login endpoint validates credentials.
- [ ] Protected endpoints require authentication.
- [ ] Logout clears frontend session state.
- [ ] Guest guard prevents authenticated users from returning to login.

---

## 2. Mandatory Password Change

- [ ] New admin-created users have `password_change_required = true`.
- [ ] Login response exposes `passwordChangeRequired`.
- [ ] Frontend enforces `/profile` while password change is required.
- [ ] `passwordChangeGuard` is connected to authenticated child routes.
- [ ] Password change requires current password.
- [ ] New password must be confirmed.
- [ ] Successful password change sets `password_change_required = false`.

---

## 3. Authorization

Endpoint rules:

```text
/api/v1/users -> Administrator only
/api/v1/profile -> any authenticated user
```

Minimum expected behavior:

```text
Read Only must not create/update/delete.
Maintenance Staff must not manage users or organization settings.
Property Manager must not manage global admin-only settings.
Administrator can manage organization-level data.
```

---

## 4. Multi-tenant isolation

For each module, verify no user can access another organization's records by changing IDs in the URL.

Modules:

- [ ] Users Management.
- [ ] Profile.
- [ ] Properties.
- [ ] Property Images.
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

---

## 5. File and AI security

- [ ] S3 bucket is private.
- [ ] Backend never exposes permanent S3 public URLs.
- [ ] Pre-signed URLs expire.
- [ ] Upload size is limited.
- [ ] Upload content types are validated.
- [ ] File access validates organization ownership.
- [ ] RAG only searches documents from current organization.
- [ ] AI does not execute free SQL.
- [ ] Missing information is reported honestly.

---

## 6. Secrets

Never commit:

```text
JWT secrets
Database passwords
AWS keys
OpenAI keys
Production URLs with credentials
Private bucket policies
```

Search before release:

```bash
git grep -nE "AKIA|SECRET|OPENAI_API_KEY|AWS_SECRET_ACCESS_KEY|JWT_SECRET|password="
```
