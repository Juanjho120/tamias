# 15 — Organization Administration and Global UX Polish

## Status

Completed through 15F.

## Purpose

Add organization administration, organization logo support, multi-organization navigation and global UX polish before starting the larger Reports, Notifications and Blueprint Analysis phases.

This phase continues the SaaS foundation already present in TAMIAS and improves the day-to-day experience for organization-aware users.

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
- AI read-only tool support should stay maintainable by decomposing the large shared support class before starting Reports.

## 15A — Organization logo backend + current organization header

### Status

Completed.

## 15B — Organization administration page

### Status

Completed.

## 15C — Organization switcher / multi-organization navigation

### Status

Completed and extended by 15C.1 / 15C.2.

### Goal

Allow users who belong to more than one organization to change the active organization from the UI.

### Backend scope

- Add `GET /api/v1/auth/organizations` to list the authenticated user's active organizations.
- Add `POST /api/v1/auth/switch-organization` to switch the active organization.
- Validate that normal users belong to the target organization and that both the membership and organization are active.
- Return an updated authenticated session/token for the selected organization.
- Keep `/api/v1/auth/me` aligned with the organization id present in the active JWT.
- Store each user's last selected organization in `users.last_organization_id`.
- Do not rely on a client-only `organizationId` override.

### Frontend scope

- Add an organization switcher to the main layout/header.
- Show organization names and the current organization logo when available.
- Switch session context when the user selects another organization.
- Refresh current user/session state after switching.
- Navigate to `/dashboard` after switching to clear organization-scoped screen state.

## 15C.1 — Global SUPER_ADMIN organization navigation

### Status

Completed.

### Goal

Allow a user with at least one active usable `SUPER_ADMIN` membership to navigate all active organizations without requiring an explicit membership in each organization.

## 15C.2 — User organization memberships management

### Status

Completed.

### Goal

Allow only `SUPER_ADMIN` users to assign users to other organizations and define the role they will have in each organization.

### Backend scope

Endpoints under the existing users backend area, protected with `SUPER_ADMIN` only:

```http
GET /api/v1/users/{userId}/organizations
POST /api/v1/users/{userId}/organizations
PUT /api/v1/users/{userId}/organizations/{organizationId}
DELETE /api/v1/users/{userId}/organizations/{organizationId}
```

### Rules

- Only `SUPER_ADMIN` can manage multi-organization memberships.
- `ADMINISTRATOR` cannot see or use these controls.
- `SUPER_ADMIN` can assign any role, including `SUPER_ADMIN`.
- The backend prevents removing a user's last usable `SUPER_ADMIN` access.
- Normal user creation/update remains scoped to the active organization.

## 15D — Icon-only action buttons with tooltips

### Status

Completed.

### Goal

Standardize action buttons in tables and modals so they display only icons while preserving accessible labels and tooltips.

### Implementation

- Shared reusable base: `IconActionButtonComponent`.
- Global enhancer: `IconActionButtonAutoEnhancerService`.
- The enhancer converts existing action buttons in tables/modals into icon-only buttons using the translated label as `title` and `aria-label`.

## 15E — TAMI branding and robot animation

### Status

Completed.

### Goal

Give the AI Assistant a TAMI identity across navigation and chat UX.

### Frontend scope

- Change the AI Assistant navigation label to `TAMI` through the existing i18n JSON files.
- Add a reusable robot identity under `shared/tami-robot`.
- Show a small robot next to the sidebar TAMI item.
- Animate the sidebar robot only on hover/focus.
- Show the same robot head next to the `/ai-assistant` title.
- Show a smaller robot in the active chat/session title.
- Animate the session title robot as if speaking while the typewriter response is running.
- Stop the speaking animation exactly when the typewriter response finishes.

## 15F — AI read-only tool support decomposition

### Status

Completed.

### Goal

Reduce the maintenance risk of the AI read-only tools by decomposing the large `AiReadOnlyToolSupport` base class before starting Phase 16 Reports.

### Backend scope

- Keep the public inheritance entry point as `AiReadOnlyToolSupport` so existing AI repositories continue to compile without broad repository rewrites.
- Split shared read-only AI support responsibilities into smaller support classes under `backend/src/main/java/com/tamias/ai/tool/support`.
- Preserve the existing AI tool behavior, routing, prompts and response style.
- Keep organization-scoped read-only behavior based on the current authenticated organization context.
- Keep `SUPER_ADMIN`-aware access helpers available to admin-oriented AI tools.

### Resulting support areas

- Base/current user context support.
- Query/scalar helpers.
- Search and text normalization helpers.
- Formatting helpers.
- Scheduled maintenance and reservation read helpers.
- Document read helpers.
- Reservation supplies and task read helpers.
- Maintenance, property and catalog read helpers.
- Purchase read helpers.
- File and image read helpers.
- AI chat history read helpers.
- User, role and access helpers.

### Rules

- No database schema changes.
- No frontend changes.
- No translation changes.
- No AI behavior changes intended.
- Do not add Reports code to this phase.

## Documentation and roadmap

This phase supersedes the previous future-phase numbering:

```text
Old future 15 Reports -> New future 16 Reports
Old future 16 Notifications/reminders -> New future 17 Notifications and reminders
Old future 17 Blueprint Analysis -> New future 18 Blueprint Analysis
```
