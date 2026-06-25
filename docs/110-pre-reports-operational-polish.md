# 110 — Pre-Reports Operational Polish

## Status

In progress through D.

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

Summary:

- Maintenance images support `BEFORE`, `AFTER` and `GENERAL` roles.
- Existing images default to `GENERAL`.
- The image modal groups images by role and role selectors stay synchronized with the real value.
- `maintenance_record_serviced_items` stores items/equipment that received maintenance.
- People, used materials and serviced items are managed from separate modals.

## B — AI sessions sort/delete, organization selector refresh and TAMI speech sync

Status: Implemented.

Summary:

- AI chat sessions can be sorted by creation date ascending or descending.
- AI chat sessions can be fully deleted with messages and debug traces.
- The organization selector refreshes after organization administration changes.
- TAMI speech audio is kept warm from user interaction so audio, mouth animation and typewriter start together more reliably.

## C — Dashboard calendar date/timezone and mobile tooltip behavior

Status: Implemented.

Summary:

- Maintenance calendar icons use `performedAt ?? scheduledAt`.
- ISO datetime values are converted to local dates before placing icons on calendar days.
- Date-only values keep their original date.
- Maintenance tooltips show formatted local date/time instead of raw ISO strings.
- Dashboard calendar tooltips use `hover/focus` on desktop and manual tap behavior on touch devices.
- Open tooltips are dismissed when tapping outside, scrolling or resizing.
- Mobile tooltips can be reopened after scroll dismissal.

## D — Maintenance last-by-person AI tool

Status: Implemented.

Implemented tool:

```text
maintenance.lastByPerson
```

Supported questions:

```text
¿Cuál fue el último mantenimiento que hizo Juanjo?
¿Cuándo fue el último mantenimiento que hizo Juanjo?
¿Cuál fue el último mantenimiento realizado por Juanjo?
```

Search behavior:

- Searches the maintenance record main responsible person through `maintenance_records.maintenance_person_id`.
- Searches involved people through `maintenance_record_people`.
- Matches against `maintenance_people.full_name`.
- Uses the selected organization from the token.
- `SUPER_ADMIN` queries are scoped to the selected organization, not all organizations.
- Does not modify data.

Ordering:

```text
COALESCE(performed_at, scheduled_at, created_at) DESC
```

Returned information:

```text
title
property
maintenance date used for ordering
status
match source
main responsible person
involved people
category
type
cost
```

Implementation notes:

- Added a dedicated handler/service/repository for this tool.
- Did not grow `AiReadOnlyToolSupport`.
- The handler is ordered before general maintenance analytics so person-specific latest maintenance questions do not fall into generic maintenance search.

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
