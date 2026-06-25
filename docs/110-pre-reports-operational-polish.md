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
- The upload role selector and the per-image role selectors must stay synchronized with the value being submitted or displayed.
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
Selector synchronization fix for upload and existing image role controls
```

### A2 — Serviced maintenance items

Maintenance now distinguishes:

```text
maintenance_record_items = items/materials used during the maintenance
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
- Serviced items are independent from the items/materials used during maintenance.

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

### A3 — Maintenance detail modal split

The maintenance detail UI was refined so each responsibility has its own action and modal.

Final UI structure:

```text
People modal
- Component: maintenance-people-modal
- Purpose: add/list/remove people involved in the maintenance.
- Table action icon: people icon.

Used materials/details modal
- Component: maintenance-details-modal
- Purpose: add/list/remove items/materials used during the maintenance.
- This modal no longer manages people.
- Table action icon: box/materials icon.

Serviced items modal
- Component: maintenance-serviced-items-modal
- Purpose: add/list/update/remove items/equipment that received maintenance.
- Table action icon: wrench-adjustable-circle icon.

Tasks
- Existing tasks behavior remains unchanged.
- The Tasks icon remains the same.
```

Rationale:

- `maintenance-details-modal` had grown to mix people and used materials.
- People are not materials, and serviced items are not used materials.
- Separate modals make the maintenance table actions clearer and reduce confusion between Tasks, Details/Materials and Serviced items.
- The Details action now means only materials/items used during maintenance.

Button/icon decision:

```text
People: people icon
Materials used / Details: box-seam icon
Serviced items: wrench-adjustable-circle icon
Tasks: list-check icon
Images: images icon
Edit: pencil-square icon
Delete: trash icon
```

## B — AI sessions sort/delete, organization selector refresh and TAMI speech sync

Status: Planned.

## C — Dashboard calendar date/timezone and mobile tooltip behavior

Status: Planned.

## D — Maintenance last-by-person AI tool

Status: Planned.

## E — Closure checks before Reports

Status: Planned.

Before starting Phase 17 — Reports, verify:

```text
- Maintenance images can be grouped as before/after/general.
- Existing maintenance images appear as general.
- Image role selectors stay synchronized with the actual image role.
- Used materials, people and serviced items are handled in separate modals.
- Serviced items are independent from used items/materials.
- AI sessions can be sorted by created date ascending/descending.
- AI sessions can be deleted without orphaning debug records.
- TAMI speaking animation and audio start together after idle.
- Organization selector refreshes after SUPER_ADMIN organization changes.
- Dashboard calendar maintenance icons use performed date first.
- Dashboard calendar no longer shifts local late-night maintenance into next UTC day.
- Dashboard/mobile tooltips dismiss predictably.
- TAMI answers last maintenance by person from responsible and involved people.
```

## Non-goals

- Do not implement Reports in this block.
- Do not implement Notifications/reminders.
- Do not implement Blueprint Analysis.
- Do not change payment processing.
- Do not add TAMI write actions.
- Do not re-grow `AiReadOnlyToolSupport`.
