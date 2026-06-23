# 15 — Organization Administration and Global UX Polish

## Status

In progress through 15D.

## Purpose

Add organization administration, organization logo support, multi-organization navigation and global UX polish before starting the larger Reports, Notifications and Blueprint Analysis phases.

## High-level decisions

- Organizations must support a logo.
- The active organization logo must appear next to the selected organization name in the main layout/header.
- A new `SUPER_ADMIN` role administers all organizations.
- Organization `ADMINISTRATOR` users can edit only their current organization.
- Users who belong to more than one organization must be able to switch the active organization from the UI.
- Organization switching must be validated server-side and must issue/update the authenticated session context.
- `SUPER_ADMIN` is global: a user with at least one active usable `SUPER_ADMIN` membership can navigate every active organization without requiring a membership row in each organization.
- Only `SUPER_ADMIN` users can assign users to other organizations and define the role they will have there.
- Action buttons in tables and modals should be icon-only with accessible labels and tooltips.
- The icon button behavior must come from a reusable base/component, not repeated one-off classes.
- The AI Assistant navigation label should be `TAMI`.
- The TAMI robot identity should be reusable in the sidebar, AI Assistant page title and active chat/session title.
- The robot in the active session title should animate as if speaking while the assistant response is being typed.

## 15A — Organization logo backend + current organization header

### Status

Completed.

## 15B — Organization administration page

### Status

Completed.

## 15C — Organization switcher / multi-organization navigation

### Status

Completed and extended by 15C.1 / 15C.2.

## 15C.1 — Global SUPER_ADMIN organization navigation

### Status

Completed.

## 15C.2 — User organization memberships management

### Status

Completed.

## 15D — Icon-only action buttons with tooltips

### Status

Completed.

### Implementation

- Added reusable `IconActionButtonComponent` for new/refactored UI code.
- Added `IconActionButtonAutoEnhancerService` to apply icon-only action button behavior across existing modules.
- The enhancer runs globally from `app.config.ts` and covers action buttons inside tables, modals, button groups and dropdown menus.
- Existing translated button text is reused as the tooltip and accessible label.
- Common text-only actions receive an inferred Bootstrap icon before the visible text is hidden.

Documentation: `docs/103-icon-action-buttons-tooltips-15d.md`.

## 15E — TAMI branding and robot animation

### Status

Planned.

## Documentation and roadmap

This phase supersedes the previous future-phase numbering:

```text
Old future 15 Reports                 -> New future 16 Reports
Old future 16 Notifications/reminders -> New future 17 Notifications and reminders
Old future 17 Blueprint Analysis      -> New future 18 Blueprint Analysis
```
