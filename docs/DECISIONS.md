# TAMIAS — Architecture and Technical Decisions

This document records important technical decisions for TAMIAS.

---

## 1. Architecture style

Decision:

```text
Use Modular Monolith for the MVP.
```

Reason:

- Lower operational complexity.
- Faster development.
- Easier deployment.
- Easier testing.
- Enough for the expected early scale.
- Still allows clean separation by domain modules.

---

## 2. Multi-tenancy strategy

Decision:

```text
Shared database + shared schema + organization_id
```

Reason:

- Simpler MVP.
- Lower cost.
- Easier reporting.
- Easier deployment.
- Good fit for early SaaS stage.

Rule:

> The backend resolves organization from authenticated user context. The frontend is not trusted as source of truth for organization isolation.

---

## 3. Backend framework

Decision:

```text
Use Java 21 + Spring Boot 3.
```

Reason:

- Strong backend stack for portfolio.
- Good ecosystem for security, JPA, validation, scheduling, S3 and AI.
- Aligns with target professional experience.

---

## 4. Frontend framework

Decision:

```text
Use Angular + TypeScript + Bootstrap.
```

Reason:

- Good fit for admin dashboards.
- Strong typed frontend.
- Reactive Forms.
- Fast UI development with Bootstrap.
- Relevant professional stack.

---

## 5. User management

Decision:

```text
Keep user administration under /users and restrict it to administrators.
```

Reason:

- User creation, role assignment and status management are administrative operations.
- Backend must enforce this with authorization.
- Frontend menu visibility is only a UX convenience, not the security source of truth.

---

## 6. Self-service profile

Decision:

```text
Add /profile as a self-service screen for every authenticated user.
```

Reason:

- Non-admin users must be able to update their own basic data.
- Users must be able to change their own password.
- This avoids requiring an administrator for basic profile updates.

Allowed fields:

```text
firstName
lastName
password
```

Not allowed from profile:

```text
email
role
status
organization
```

---

## 7. Mandatory temporary password change

Decision:

```text
New users created by an administrator must change their temporary password on first login.
```

Implementation:

```text
users.password_change_required
```

Rules:

- Existing users default to `false`.
- New admin-created users are created with `true`.
- Login response includes `passwordChangeRequired`.
- Frontend redirects users with `passwordChangeRequired = true` to `/profile`.
- User cannot continue using the rest of the app until password is changed.
- Successful password change sets `passwordChangeRequired = false`.

Reason:

- Admin-created passwords are temporary.
- Users should take ownership of their password.
- Reduces risk of shared or reused temporary credentials.

---

## 8. Inventory Items refactor

Decision:

```text
Replace the old materials catalog with Inventory Items.
```

Reason:

The old `materials` concept was too narrow. TAMIAS needs one shared operational item catalog that can support:

- maintenance usage,
- purchase lists,
- reservation supplies,
- future inventory reporting,
- future AI tool calling.

Current model:

```text
inventory_items
```

Business label:

```text
Spanish: Insumos y materiales
English: Inventory Items
```

Operational availability is controlled by:

```text
available_for_maintenance
available_for_reservations
available_for_purchases
```

---

## 9. Reservation supplies

Decision:

```text
Use reservation_supplies for items delivered to guests during reservations.
```

Reason:

- Supplies are operationally different from reservation header data.
- Supplies may be added/edited after reservation creation.
- Supplies should be reportable.
- Supplies should be queryable by future AI tools.

Frontend decision:

```text
Use a separate Supplies modal from the Reservations table.
```

---

## 10. Reports

Decision:

```text
Do not implement JasperReports in the MVP.
```

Reason:

- More valuable to first complete stable operational flows.
- Reports can build on existing clean data later.
- Dashboard analytics should come before full PDF reporting.

---

## 11. Tool Calling

Decision:

```text
Tool Calling will be added after MVP hardening and analytics foundations.
```

Reason:

- Tool calling should run over stable domain services.
- Read-only tools should come first.
- No SQL should be exposed directly to the model.

Initial tool candidates:

```text
findLastPurchaseByInventoryItem(itemName)
getMaintenanceCostByYear(year)
findOverdueTasks()
findLastMaintenanceByCategory(categoryName)
getUpcomingScheduledMaintenance(days)
getReservationSuppliesByReservation(reservationCode)
```
