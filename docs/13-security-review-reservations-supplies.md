# TAMIAS — Security Review 6B-4: Reservations and Reservation Supplies

## Scope

This review covers:

```text
Reservations
Reservation Guests
Reservation Supplies
Reservation calendar
Reservation cancellation
Reservation deletion
```

---

## Findings

### 1. Reservations already enforce organization isolation

`ReservationService` already uses:

```text
currentUserService.getCurrentOrganizationId()
findByIdAndOrganization_IdAndDeletedAtIsNull(...)
```

and validates related property and platform through organization-scoped repository methods.

This is correct for multi-tenant isolation.

---

### 2. Reservation requests allowed DELETED status through create/update

`ReservationRequest` includes:

```text
ReservationStatus status
```

and `ReservationStatus` includes:

```text
DELETED
```

Risk:

```text
POST /api/v1/reservations
PUT /api/v1/reservations/{id}
```

could set:

```text
status = DELETED
```

without applying the delete flow that sets:

```text
deletedAt
deletedBy
updatedBy
```

Fix:

`ReservationService.create(...)` and `ReservationService.update(...)` now reject `ReservationStatus.DELETED`.

Deletion must go through:

```http
DELETE /api/v1/reservations/{id}
```

---

### 3. Reservation update did not revalidate property availability

Create validated overlapping reservations, but update did not call availability validation.

Risk:

```text
A reservation could be updated to overlap another active reservation for the same property.
```

Fix:

`ReservationService.update(...)` now validates availability and excludes the reservation being updated.

A new repository method was added:

```text
existsByOrganization_IdAndProperty_IdAndIdNotAndStatusNotAndDeletedAtIsNullAndCheckInLessThanAndCheckOutGreaterThan(...)
```

---

### 4. Reservation supplies now revalidate the parent reservation before mutation

`updateSupply(...)` and `deleteSupply(...)` were organization-scoped through the supply row, but they did not first revalidate the parent reservation with:

```text
findByIdAndOrganization_IdAndDeletedAtIsNull(...)
```

Fix:

Both methods now call `findEntityInCurrentOrganization(reservationId)` first.

This prevents mutating supplies for a deleted reservation.

---

### 5. Reservation supplies now enforce inventory item status

`InventoryItem` already had:

```text
availableForReservations
```

and the service already validated that flag.

Fix added:

The backend now also validates:

```text
CatalogStatus.ACTIVE
availableForReservations = true
same organization
not deleted
```

This prevents inactive inventory items from being used as reservation supplies.

---

## Files changed

```text
backend/src/main/java/com/tamias/reservation/service/ReservationService.java
backend/src/main/java/com/tamias/reservation/repository/ReservationRepository.java
docs/13-security-review-reservations-supplies.md
```

---

## Validation

Backend build:

```bash
cd backend
./mvnw.cmd clean package -DskipTests
```

Manual API smoke tests:

```text
1. Login as Read Only.
2. GET /api/v1/reservations -> allowed.
3. GET /api/v1/reservations/calendar -> allowed.
4. GET /api/v1/reservations/{id}/supplies -> allowed.
5. POST /api/v1/reservations -> forbidden.
6. POST /api/v1/reservations/{id}/supplies -> forbidden.

7. Login as Property Manager.
8. POST /api/v1/reservations with status DELETED -> 400.
9. PUT /api/v1/reservations/{id} with status DELETED -> 400.
10. Create reservation A for property X and date range.
11. Try to update reservation B to overlap reservation A -> 409.
12. Update reservation A without changing dates -> allowed.
13. Add supply with inactive inventory item -> 400.
14. Add supply with availableForReservations=false -> 400.
15. Add supply with active availableForReservations=true item -> allowed.
16. DELETE /api/v1/reservations/{id} -> soft delete.
```
