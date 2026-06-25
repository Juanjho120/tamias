# 110 — Pre-Reports Operational Polish

## Status

In progress through A.

## Purpose

Before starting Phase 17 — Reports, TAMIAS needs a small stabilization and operational polish block. These changes improve maintenance evidence, maintenance item tracking, AI chat session management, TAMI speech behavior, organization switching, dashboard date accuracy and mobile tooltip behavior.

This is not a new numbered product phase. Reports remains Phase 17. This block exists to avoid starting Reports on top of known UX/data issues.

## Scope summary

Requested improvements:

```text
1. Maintenance images must support BEFORE, AFTER and GENERAL grouping.
2. Maintenance must distinguish items used from items/equipment serviced.
3. AI chat sessions must support ascending/descending ordering by creation date and complete session deletion.
4. TAMI speech audio must start in sync with the speaking animation after idle periods.
5. TAMI must answer last maintenance by person, searching both the main responsible person and involved people.
6. The organization switcher must refresh after a SUPER_ADMIN creates/deletes organizations.
7. Dashboard calendar maintenance icons must use performed date first, scheduled date as fallback, and avoid UTC date shifts.
8. Dashboard/mobile tooltips must be easier to dismiss and should not remain stuck after tap.
```

## A — Maintenance before/after/general images and serviced items

Status: Implemented.

### A1 — Maintenance image roles

Maintenance images now support an explicit role/group:

```text
BEFORE
AFTER
GENERAL
```

Database change:

```text
V41__maintenance_image_roles_and_serviced_items.sql
```

The migration adds:

```text
maintenance_record_images.image_role varchar(20) not null default 'GENERAL'
```

Rules:

- Existing images default to `GENERAL`.
- Upload accepts a selected role.
- Existing images can be reassigned between `BEFORE`, `AFTER` and `GENERAL`.
- The maintenance images modal groups images by role.
- S3 behavior does not change.
- Deletion remains hard delete: object removed from S3 and row removed from database.
- No images are stored in `bytea`.

Backend additions:

```text
MaintenanceImageRole enum
MaintenanceRecordImageRoleRequest DTO
PATCH /api/v1/maintenance-records/{maintenanceRecordId}/images/{imageId}/role
```

Frontend additions:

```text
Upload role selector
Per-image role selector
Grouped image sections: Before, After, General
```

### A2 — Serviced maintenance items

Maintenance now distinguishes:

```text
maintenance_record_items = items used during the maintenance
maintenance_record_serviced_items = items/equipment that received maintenance
```

New table:

```text
maintenance_record_serviced_items
```

Fields:

```text
id
organization_id
maintenance_record_id
inventory_item_id nullable
item_name_snapshot
quantity
unit
notes
created_by
created_at
updated_by
updated_at
deleted_by
deleted_at
```

Rules:

- Serviced items use soft delete.
- `inventory_item_id` is nullable to allow manual/free-text serviced items.
- `item_name_snapshot` preserves the label used at maintenance time.
- If an inventory item is selected, it must belong to the selected organization and be available for maintenance.
- Organization scoping follows the maintenance record.

Backend endpoints:

```http
GET    /api/v1/maintenance-records/{maintenanceRecordId}/serviced-items
POST   /api/v1/maintenance-records/{maintenanceRecordId}/serviced-items
PUT    /api/v1/maintenance-records/{maintenanceRecordId}/serviced-items/{servicedItemId}
DELETE /api/v1/maintenance-records/{maintenanceRecordId}/serviced-items/{servicedItemId}
```

Frontend wording:

```text
Items usados
Items con mantenimiento
```

## B — AI sessions sort/delete, organization selector refresh and TAMI speech sync

Status: Planned.

## C — Dashboard calendar date/timezone and mobile tooltip behavior

Status: Planned.

## D — Maintenance last-by-person AI tool

Status: Planned.

## E — Closure checks before Reports

Status: Planned.

## Non-goals

- Do not implement Reports in this block.
- Do not implement Notifications/reminders.
- Do not implement Blueprint Analysis.
- Do not change payment processing.
- Do not add TAMI write actions.
- Do not re-grow `AiReadOnlyToolSupport`.
