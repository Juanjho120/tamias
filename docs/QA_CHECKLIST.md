# TAMIAS — Manual QA Checklist

Use this checklist before demo, deployment, or large refactors.

---

## 1. Setup

- [ ] `docker compose up -d` starts PostgreSQL and Chroma.
- [ ] Backend starts with local environment.
- [ ] Frontend starts with `npm start`.
- [ ] Login page loads with TAMIAS logo.
- [ ] No browser console errors on first load.

---

## 2. Authentication

- [ ] Valid login works.
- [ ] Invalid login shows error.
- [ ] Logout works.
- [ ] Protected route redirects to login after logout.
- [ ] Token is sent in authenticated API requests.
- [ ] Authenticated users are redirected away from `/login`.

---

## 3. Mandatory Password Change

- [ ] Admin creates a new user.
- [ ] New user logs in with temporary password.
- [ ] New user is redirected to `/profile`.
- [ ] New user cannot manually navigate to `/dashboard`.
- [ ] Current password is required.
- [ ] New password confirmation is required.
- [ ] Mismatched password confirmation shows validation.
- [ ] Password change succeeds.
- [ ] User is redirected to `/dashboard`.
- [ ] User can log in again with the new password.
- [ ] Password change is no longer forced.

---

## 4. Profile

- [ ] Administrator can open `/profile`.
- [ ] Property Manager can open `/profile`.
- [ ] Maintenance Staff can open `/profile`.
- [ ] Read Only can open `/profile`.
- [ ] First name can be updated.
- [ ] Last name can be updated.
- [ ] Password can be changed.
- [ ] Email cannot be changed from profile.
- [ ] Role cannot be changed from profile.

---

## 5. Users Management

- [ ] Users menu appears for Administrator.
- [ ] Users menu does not appear for non-admin users.
- [ ] Administrator can open `/users`.
- [ ] Administrator can create user.
- [ ] Administrator can edit user role.
- [ ] Administrator can edit user status.
- [ ] Administrator can activate user.
- [ ] Administrator can deactivate user.
- [ ] Administrator can delete user.
- [ ] Administrator cannot delete own user from UI.
- [ ] Non-admin direct API access to users endpoint is rejected.

---

## 6. Core operational smoke test

- [ ] Properties CRUD.
- [ ] Inventory Items CRUD.
- [ ] Maintenance record items.
- [ ] Scheduled maintenance status changes.
- [ ] Reservations CRUD.
- [ ] Reservation Supplies modal.
- [ ] Task lists and task items.
- [ ] Purchase lists and purchase items.
- [ ] Documents upload/download.
- [ ] AI assistant chat.
- [ ] Dashboard calendar.

---

## 7. Final checks

- [ ] Backend build passes.
- [ ] Frontend build passes.
- [ ] CI passes.
- [ ] No uncommitted files.
- [ ] README/docs are current.
