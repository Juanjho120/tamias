# 110 — Pre-Reports Operational Polish

## Status

Planned / next.

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

### A1 — Maintenance image roles

Current maintenance images are stored as flat images. Add a role/group field to maintenance record images:

```text
image_role: BEFORE | AFTER | GENERAL
```

Recommended database change:

```text
ALTER TABLE maintenance_record_images
ADD COLUMN image_role varchar(20) NOT NULL DEFAULT 'GENERAL';
```

Rules:

- Existing images default to `GENERAL`.
- Upload must allow selecting the target role.
- Existing images must allow changing role after upload.
- The UI modal must group images clearly: Before, After and General / ungrouped.
- S3 behavior does not change.
- Image deletion remains hard delete where the current maintenance image module already uses hard delete.
- Do not store images in `bytea`.

Backend scope:

```text
- Add MaintenanceImageRole enum.
- Add imageRole field to maintenance image entity/DTOs.
- Accept role on upload.
- Add endpoint or update endpoint to change an image role.
- Keep organization ownership checks.
```

Frontend scope:

```text
- Add role selector during upload.
- Add per-image role control under each image.
- Group images by role in the modal.
- Use existing modal/snackbar patterns.
- Do not use native confirm().
```

### A2 — Serviced maintenance items

Current maintenance item tracking represents items used during maintenance. A separate relation is needed for items/equipment that received maintenance.

Recommended table name:

```text
maintenance_record_serviced_items
```

Recommended fields:

```text
id
organization_id
maintenance_record_id
inventory_item_id nullable
item_name_snapshot
quantity nullable/default 1
notes nullable
created_by
created_at
updated_by
updated_at
deleted_by
deleted_at
```

Rules:

- `maintenance_record_items` remains for items used.
- `maintenance_record_serviced_items` is for items/equipment serviced.
- Use soft delete for serviced item rows.
- `inventory_item_id` can be nullable to allow free-text serviced items if needed.
- `item_name_snapshot` preserves the label shown at maintenance time.
- Organization scoping must match the maintenance record organization.
- If an inventory item is selected, it must belong to the same organization.

Frontend wording:

```text
Items used
Items serviced
```

Spanish wording:

```text
Items usados
Items con mantenimiento
```

Recommended default Spanish UI label: `Items con mantenimiento`.

### A3 — Flyway

The expected migration for this block is:

```text
V41__maintenance_image_roles_and_serviced_items.sql
```

Before implementation, verify again that `V41` does not already exist.

## B — AI sessions sort/delete, organization selector refresh and TAMI speech sync

### B1 — AI chat session sorting

Current behavior sorts sessions by `updatedAt,desc`. Add explicit UI sorting by creation date:

```text
createdAt descending
createdAt ascending
```

Backend may already support pageable sorting if Spring Data pagination is used. Prefer using existing query/sort support before adding new endpoints.

Frontend scope:

```text
- Add sort selector in the session list/sidebar area.
- Persist only in component state unless a preference system already exists.
- Reload sessions when sort changes.
```

### B2 — AI chat session deletion

Add full session deletion.

Backend scope:

```text
DELETE /api/v1/ai/chat-sessions/{sessionId}
```

Rules:

- Only the owner of the session can delete it.
- Delete messages and debug traces safely.
- Prefer transactional deletion.
- Confirm database FK behavior before implementing.
- Do not leave orphan `ai_chat_message_debugs`.

Frontend scope:

```text
- Add delete action beside session title/edit action.
- Use existing confirm modal.
- If deleting the active session, select another existing session or start a new empty conversation.
- Show snackbar/toast on success/error.
```

### B3 — Organization selector refresh

Problem:

When a `SUPER_ADMIN` creates or deletes/deactivates an organization, the organization switcher does not refresh until full page reload.

Recommended approach:

```text
- Add a lightweight organization-switcher refresh signal in an existing shared service.
- Organizations page emits refresh after create/update/delete/reactivate.
- Main layout listens and reloads organization options.
```

Rules:

- Do not rely on `F5`.
- Do not reload the whole application unless needed.
- If the current organization is deleted/deactivated, move the user to a valid available organization or force a safe refresh flow already supported by the backend.

### B4 — TAMI speech audio sync

Problem:

After idle time, browser audio may be suspended. The speaking animation starts immediately, but audio can start late.

Recommended approach:

```text
- Prepare/resume the AudioContext before setting the speaking state.
- Trigger an immediate first blip after resume.
- Start animation and audio from the same lifecycle point.
- Keep all audio generation original/synthetic; do not use copyrighted game assets.
```

Rules:

- Do not use external audio assets copied from games.
- Keep the sound generated locally with Web Audio API.
- Stop sound when typewriter stops, session changes or component is destroyed.

## C — Dashboard calendar date/timezone and mobile tooltip behavior

### C1 — Maintenance calendar date selection

Calendar maintenance icon date must use:

```text
performedAt ?? scheduledAt
```

Rules:

- If maintenance has an execution/performed date, display it on the execution date.
- If execution/performed date is null, display it on scheduled date.
- Tooltips should label the date according to the same logic.

### C2 — Local timezone conversion

Problem example:

```text
User selected performed time: 2026-06-24 23:00 Guatemala
Stored/displayed ISO: 2026-06-25T05:00:00Z
Calendar shows icon on 2026-06-25
Expected calendar day: 2026-06-24
```

Recommended frontend rule:

```text
- For date-only values `YYYY-MM-DD`, use the string directly.
- For datetime values, convert through `new Date(value)` and extract local date parts.
- Do not use `String(value).slice(0, 10)` for ISO datetime values.
```

Also search for the same risky pattern in other frontend modules.

### C3 — Tooltip behavior on touch devices

Problem:

Bootstrap tooltips can remain stuck on mobile tap interactions.

Recommended behavior:

```text
- Desktop: hover/focus.
- Touch/mobile: click/tap with explicit outside-tap dismissal.
- Hide visible tooltips on scroll, month change, route change, modal open and tap outside.
```

Implementation direction:

```text
- Start with dashboard calendar tooltips.
- If the same problem exists with global icon-action tooltips, extend the shared tooltip enhancer.
```

Rules:

- Avoid tooltips that trap mobile users.
- Do not require tapping the same icon twice.
- Keep keyboard/focus accessibility for desktop.

## D — Maintenance last-by-person AI tool

Add or extend read-only AI maintenance tools to answer:

```text
¿Cuál fue el último mantenimiento que hizo Juanjo?
¿Cuándo fue el último mantenimiento que hizo Juanjo?
```

Search target:

```text
- Main responsible person of the maintenance record.
- Involved people linked through maintenance_record_people.
```

Recommended tool:

```text
maintenance.lastByPerson
```

Ordering:

```text
COALESCE(performed_at, scheduled_at, created_at) DESC
```

Rules:

- Organization-scoped through the selected organization in the token.
- `SUPER_ADMIN` uses the selected organization.
- Do not grow `AiReadOnlyToolSupport`; add repository/handler logic in the maintenance AI area or a dedicated support class if needed.
- Return maintenance title/name, date used, person match source, property, category/type if available and brief notes if useful.
- If no match is found, state that no maintenance was found for that person.

## E — Closure checks before Reports

Before starting Phase 17 — Reports, verify:

```text
- Maintenance images can be grouped as before/after/general.
- Existing maintenance images appear as general.
- Serviced items are independent from used items.
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
