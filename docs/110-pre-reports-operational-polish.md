# 110 — Pre-Reports Operational Polish

## Status

In progress through C.

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

- Maintenance images now support `BEFORE`, `AFTER` and `GENERAL` roles.
- Existing images default to `GENERAL`.
- The image modal groups images by role and role selectors stay synchronized with the real value.
- `maintenance_record_serviced_items` stores items/equipment that received maintenance.
- People, used materials and serviced items are managed from separate modals.

## B — AI sessions sort/delete, organization selector refresh and TAMI speech sync

Status: Implemented.

### B1 — AI chat session sorting

AI chat sessions can now be sorted by creation date from the AI Assistant page.

Supported UI sort options:

```text
Created date descending
Created date ascending
```

Implementation notes:

- The existing pageable backend endpoint is reused.
- The frontend passes `sort=createdAt,desc` or `sort=createdAt,asc`.
- Sorting resets the session page back to the first page.
- No database changes are needed.

### B2 — AI chat session deletion

A full session delete action is now available from the AI Assistant session list.

Backend endpoint:

```http
DELETE /api/v1/ai/chat-sessions/{sessionId}
```

Rules:

- Only the session owner in the selected organization can delete the session.
- Message debug rows are deleted first.
- Chat messages are deleted second.
- The session row is deleted last.
- The delete operation is transactional.
- No native `confirm()` is used; the frontend uses the shared confirm modal.

### B3 — Organization selector refresh

The organization switcher refreshes when organization administration changes the available organization list.

Implemented behavior:

```text
- Create organization -> selector reloads without F5.
- Update organization name/logo -> selector reloads without F5.
- Activate/deactivate organization -> selector reloads without F5.
- Delete organization logo -> selector reloads without F5.
```

Implementation notes:

- `AuthService` exposes a lightweight organization options refresh observable.
- `OrganizationsPageComponent` emits refresh events after organization mutations.
- `MainLayoutComponent` listens and reloads organization options.

### B4 — TAMI speech sync

TAMI speech audio is now prepared before the typewriter starts the visible speaking cursor.

Implementation notes:

- `TamiSpeechAudioService` exposes `prepare()` to resume/warm the browser `AudioContext`.
- The AI Assistant page calls `prepare()` from the user send interaction.
- The actual typewriter cursor is enabled after `start()` has attempted to resume audio and triggered the first blip.
- This reduces the idle-delay where the mouth animation starts before sound.
- The audio remains synthetic/original and does not use copyrighted game assets.

## C — Dashboard calendar date/timezone and mobile tooltip behavior

Status: Implemented.

Implemented behavior:

- Maintenance calendar icons use `performedAt ?? scheduledAt`.
- ISO datetime values are converted to local dates before placing icons on calendar days.
- Date-only values keep their original date.
- Maintenance tooltips show formatted local date/time instead of raw ISO strings.
- Dashboard calendar tooltips use `hover/focus` on desktop and `click` on touch devices.
- Open tooltips are dismissed when tapping outside, scrolling or resizing.

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
