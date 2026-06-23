# 15 — Organization Administration and Global UX Polish

## Status

In progress through 15B.

## Purpose

Add organization administration, organization logo support, multi-organization navigation and global UX polish before starting the larger Reports, Notifications and Blueprint Analysis phases.

This phase continues the SaaS foundation already present in TAMIAS and improves the day-to-day experience for organization-aware users.

## High-level decisions

- Organizations must support a logo.
- The active organization logo must appear next to the selected organization name in the main layout/header.
- A new `SUPER_ADMIN` role will be introduced for platform-level organization administration.
- Organization `ADMINISTRATOR` users can edit only their current organization.
- Users who belong to more than one organization must be able to switch the active organization from the UI.
- Organization switching must be validated server-side and must issue/update the authenticated session context.
- Action buttons in tables and modals should be icon-only with accessible labels and tooltips.
- The icon button behavior must come from a reusable base/component, not repeated one-off classes.
- The AI Assistant navigation label should be `TAMI`.
- The TAMI robot identity should be reusable in the sidebar, AI Assistant page title and active chat/session title.
- The robot in the active session title should animate as if speaking while the assistant response is being typed.

## 15A — Organization logo backend + current organization header

### Status

Completed.

### Goal

Allow each organization to store logo metadata and show the active organization logo next to the selected organization name.

### Backend scope

- Add organization logo metadata columns through Flyway.
- Update the `Organization` entity.
- Update organization response DTOs.
- Update authentication/session organization response DTOs so the current organization can expose a presigned logo URL.
- Reuse the existing storage/S3 service and image validation patterns.
- Add endpoints for current organization logo upload/replacement and deletion.
- Keep organization logo files private in storage.
- Use presigned URLs for display.
- Hard-delete the previous logo object from storage when replacing or deleting it.

### Frontend scope

- Update auth/session models for organization logo data.
- Show the active organization logo next to the active organization name.
- Use a fallback initials badge when no logo exists.
- Refresh the current session/user organization data after changing the logo.

## 15B — Organization administration page

### Status

Completed.

### Goal

Add a UI to administer organizations with different behavior for `SUPER_ADMIN` and organization `ADMINISTRATOR` users.

### Roles and permissions

#### SUPER_ADMIN

- Can view and administer all organizations.
- Can create, update, activate/deactivate and edit organization logo data.

#### Organization ADMINISTRATOR

- Can edit only the current organization.
- Can upload/replace/delete only the current organization logo.
- Cannot access or modify other organizations.
- Cannot create, activate or deactivate organizations.
- Cannot assign `SUPER_ADMIN` through user management.

### Backend scope

- Introduce `SUPER_ADMIN` in the existing role/security model.
- Add read/write endpoints for platform organization administration only when authorized as `SUPER_ADMIN`.
- Keep current organization endpoints available for organization `ADMINISTRATOR` users.
- Validate tenant boundaries consistently.
- Harden user role assignment so only `SUPER_ADMIN` can assign the `SUPER_ADMIN` role.

### Frontend scope

- Add organization administration route/page at `/organizations`.
- Show platform-wide organization management only to `SUPER_ADMIN`.
- Show current-organization editing to organization `ADMINISTRATOR`.
- Include logo upload/replace/delete controls.
- Reuse existing modal/snackbar patterns.
- Do not use native `confirm()`.

### Non-goals

- Do not implement organization switching in 15B.
- Do not let normal users edit organizations.
- Do not allow organization `ADMINISTRATOR` users to view or edit other organizations.

## 15C — Organization switcher / multi-organization navigation

### Status

Planned.

### Goal

Allow users who belong to more than one organization to change the active organization from the UI.

### Backend scope

- Add an endpoint to list the authenticated user's active organizations.
- Add an endpoint to switch the active organization.
- Validate that the user belongs to the target organization and that both the membership and organization are active.
- Return an updated authenticated session/token for the selected organization.
- Do not rely on a client-only `organizationId` override.

### Frontend scope

- Add an organization switcher to the main layout/header.
- Show organization names and logos when available.
- Switch session context when the user selects another organization.
- Refresh current user/session state after switching.
- Clear or reload organization-scoped screen state after switching.

### Security notes

The active organization must be resolved by the backend from the authenticated context. A frontend-only selected organization id is not sufficient for authorization.

## 15D — Icon-only action buttons with tooltips

### Status

Planned.

### Goal

Standardize action buttons in tables and modals so they display only an icon and show the action name in a tooltip on hover/focus.

### Required actions to cover over time

```text
Images
Edit
Delete
3D Box Models
Faces
Upload original
Upload image
Replace
Detect contour
Items
Tasks
Details
Supplies
Cancel
Deactivate
Open
Process
Index
Chunks
History
Reschedule
Pause
Generate record
```

### Frontend scope

- Create a reusable icon action button base/component.
- Support translated tooltip text.
- Support accessible labels through `aria-label`.
- Support disabled state.
- Support loading state when needed.
- Support variants consistent with the current Bootstrap UI.
- Migrate action buttons gradually by module.

## 15E — TAMI branding and robot animation

### Status

Planned.

### Goal

Improve TAMI's visual identity and replace the generic `AI Assistant` naming in navigation.

### Frontend scope

- Show `TAMI` instead of `AI Assistant` in the sidebar/navigation.
- Add a small reusable TAMI robot head/body next to the sidebar label.
- Trigger the sidebar robot animation on hover.
- Show the same robot head, static, next to the `TAMI` title on `/ai-assistant`.
- Show the same robot head in the active chat/session title with a speaking animation while the assistant response is being typed.
- Start the speaking animation when the typewriter response starts.
- Stop the speaking animation when the typewriter response completes.

### Technical direction

- Prefer CSS/SVG/HTML for the robot identity instead of heavy GIFs or raster assets.
- Keep the robot reusable through a small shared component.
- Keep animation state controlled by the AI Assistant component so it can be synchronized with the typewriter lifecycle.

## Documentation and roadmap

This phase supersedes the previous future-phase numbering:

```text
Old future 15 Reports -> New future 16 Reports
Old future 16 Notifications/reminders -> New future 17 Notifications and reminders
Old future 17 Blueprint Analysis -> New future 18 Blueprint Analysis
```

## Verification checklist

- Roadmap shows Phase 15 as Organization Administration and Global UX Polish.
- Reports, Notifications and Blueprint Analysis are moved to phases 16, 17 and 18.
- Subphases 15A through 15E are documented.
- Security decisions for `SUPER_ADMIN` vs organization `ADMINISTRATOR` are documented.
- Organization switcher security expectations are documented.
- Icon-only button accessibility expectations are documented.
- TAMI robot animation lifecycle expectations are documented.
