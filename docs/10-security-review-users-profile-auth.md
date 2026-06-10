# TAMIAS — Security Review 6B-1: Users, Profile and Auth

## Scope

This review covers:

```text
Auth
JWT filter
Users Management
My Profile
Mandatory temporary password change
Frontend route guards
```

---

## Findings

### 1. Password change guard was not applied to authenticated routes

The frontend already had:

```text
password-change.guard.ts
```

but `app.routes.ts` had `canActivateChild` attached to the login route instead of the authenticated layout route.

Risk:

```text
A user with passwordChangeRequired = true could manually navigate to /dashboard.
```

Fix:

```ts
canActivateChild: [passwordChangeGuard]
```

was moved to the authenticated layout route.

---

### 2. JWT filter trusted token claims without checking current database state

The JWT filter validated token signature and expiration, then used claims from the token.

Risk:

```text
If a user is deactivated, locked, deleted, or has role changed after token issuance,
the old token may continue working until expiration.
```

Fix:

The JWT filter now reloads the user through `UserDetailsService` on each request.

This means current database state is used for:

```text
user status
active organization
current role
```

---

### 3. Auth /me did not validate active status

`AuthService.getCurrentUserResponse(...)` loaded the user by id without checking active/deleted state.

Fix:

It now uses:

```text
findByIdAndDeletedAtIsNull
UserStatus.ACTIVE
active UserOrganization
```

---

### 4. Admin could change own role/status through API

The UI prevented self deactivate/delete, but backend still allowed a direct API call to change own role/status.

Risk:

```text
An administrator could accidentally lock themselves out or downgrade their own role.
```

Fix:

`UserService.update(...)` blocks changing current user's own role or status.

---

### 5. Users update allowed DELETED status through PUT

`UserUpdateRequest` supports `UserStatus`, including `DELETED`.

Risk:

```text
PUT /users/{id} could set status DELETED without applying the same logic as DELETE.
```

Fix:

`UserService.update(...)` now rejects `status = DELETED`.

Deletion must go through:

```http
DELETE /api/v1/users/{id}
```

---

## Files changed

```text
frontend/src/app/app.routes.ts
backend/src/main/java/com/tamias/security/jwt/JwtAuthenticationFilter.java
backend/src/main/java/com/tamias/auth/service/AuthService.java
backend/src/main/java/com/tamias/user/service/UserService.java
docs/10-security-review-users-profile-auth.md
```

---

## Validation

### Frontend

```bash
cd frontend
npm run build
npm start
```

Test:

```text
1. Create a new user.
2. Login with temporary password.
3. Confirm redirect to /profile.
4. Try to open /dashboard manually.
5. Confirm redirect back to /profile.
6. Change password.
7. Confirm access to dashboard.
```

### Backend

```bash
cd backend
./mvnw.cmd clean package -DskipTests
```

Manual API tests:

```text
1. Login as admin.
2. Try to PUT your own user with status INACTIVE.
3. Expected: 400.
4. Try to PUT your own user with role READ_ONLY.
5. Expected: 400.
6. Try to PUT another user with status DELETED.
7. Expected: 400.
8. Delete another user with DELETE /users/{id}.
9. Expected: 204.
10. Deactivate a user.
11. Try old token from that user.
12. Expected: 401 or blocked by authentication.
```
