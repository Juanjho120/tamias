# TAMIAS — Security Review 6B-3: Maintenance and Scheduled Maintenance

## Scope

This review covers:

```text
Maintenance Records
Maintenance Record People
Maintenance Record Items
Scheduled Maintenance
Scheduled Maintenance History
Generated Maintenance Records
```

---

## Findings

### 1. Maintenance records already enforce organization isolation

`MaintenanceRecordService` uses:

```text
currentUserService.getCurrentOrganizationId()
findByIdAndOrganization_IdAndDeletedAtIsNull(...)
```

and validates related property, category, type and person through organization-scoped repository methods.

This is correct for multi-tenant isolation.

---

### 2. Maintenance records allowed DELETED status through create/update

`MaintenanceRecordRequest` includes:

```text
MaintenanceStatus status
```

and `MaintenanceStatus` includes:

```text
DELETED
```

Risk:

```text
POST /api/v1/maintenance-records
PUT /api/v1/maintenance-records/{id}
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

`MaintenanceRecordService.create(...)` and `MaintenanceRecordService.update(...)` now reject `MaintenanceStatus.DELETED`.

Deletion must go through:

```http
DELETE /api/v1/maintenance-records/{id}
```

---

### 3. Maintenance detail mutations now revalidate the parent record

`MaintenanceDetailService` already scoped item/person operations by organization.

However, some mutations validated the child row but did not revalidate that the parent maintenance record was still active/not deleted.

Fix:

These methods now call `validateMaintenanceRecord(...)` before mutation:

```text
removePerson
updateItem
removeItem
```

This keeps behavior consistent with:

```text
findPeople
addPerson
findItems
addItem
```

---

### 4. Maintenance items now enforce inventory item availability

Maintenance details can reference `InventoryItem`.

Fix:

When an `inventoryItemId` is used in a maintenance detail, the backend now validates:

```text
CatalogStatus.ACTIVE
availableForMaintenance = true
same organization
not deleted
```

This prevents using reservation-only or inactive inventory items in maintenance records.

---

### 5. Scheduled maintenance already enforces organization isolation

`ScheduledMaintenanceService` uses:

```text
currentUserService.getCurrentOrganizationId()
findByIdAndOrganization_IdAndDeletedAtIsNull(...)
```

and validates related property, category, type and person through organization-scoped repository methods.

This is correct for multi-tenant isolation.

---

### 6. Scheduled maintenance allowed DELETED status through create/update

`ScheduledMaintenanceRequest` includes:

```text
ScheduledMaintenanceStatus status
```

and `ScheduledMaintenanceStatus` includes:

```text
DELETED
```

Risk:

```text
POST /api/v1/scheduled-maintenance
PUT /api/v1/scheduled-maintenance/{id}
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
history entry
```

Fix:

`ScheduledMaintenanceService.create(...)` and `ScheduledMaintenanceService.update(...)` now reject `ScheduledMaintenanceStatus.DELETED`.

Deletion must go through:

```http
DELETE /api/v1/scheduled-maintenance/{id}
```

---

### 7. Scheduled maintenance history is organization-scoped

History reads use:

```text
findByScheduledMaintenance_IdAndOrganization_IdOrderByChangedAtDesc(...)
```

and the parent scheduled maintenance is resolved first using the current organization.

This is correct.

---

## Files changed

```text
backend/src/main/java/com/tamias/maintenance/service/MaintenanceRecordService.java
backend/src/main/java/com/tamias/maintenance/detail/service/MaintenanceDetailService.java
backend/src/main/java/com/tamias/scheduledmaintenance/service/ScheduledMaintenanceService.java
docs/12-security-review-maintenance-scheduled-maintenance.md
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
2. GET /api/v1/maintenance-records -> allowed.
3. GET /api/v1/scheduled-maintenance -> allowed.
4. POST /api/v1/maintenance-records -> forbidden.
5. POST /api/v1/scheduled-maintenance -> forbidden.

6. Login as Maintenance Staff.
7. POST /api/v1/maintenance-records -> allowed.
8. POST /api/v1/maintenance-records with status DELETED -> 400.
9. Add maintenance item using an inactive inventory item -> 400.
10. Add maintenance item using availableForMaintenance=false -> 400.

11. Login as Property Manager.
12. POST /api/v1/scheduled-maintenance with status DELETED -> 400.
13. PUT /api/v1/scheduled-maintenance/{id} with status DELETED -> 400.
14. DELETE /api/v1/scheduled-maintenance/{id} -> soft delete with history.
```
