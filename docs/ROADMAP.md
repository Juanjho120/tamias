# TAMIAS — Roadmap

This roadmap reflects the current state after AI tool calling/orchestration, S3/image hard-delete work, inventory brand/image work, AI debug traces, smoke-test hardening, Product Box Models through AI awareness and Phase 15 organization/global UX work.

## Completed foundation

### Phase 0 — Project Setup

Status: Completed.

### Phase 1 — Security and SaaS Foundation

Status: Completed / MVP-ready.

### Phase 2 — Properties and Catalogs

Status: Completed / MVP-ready.

### Phase 3 — Maintenance

Status: Completed / MVP-ready.

### Phase 4 — Reservations and Tasks

Status: Completed / MVP-ready.

### Phase 5 — Purchase Lists

Status: Completed / MVP-ready.

### Phase 6 — Documents

Status: Completed / MVP-ready.

### Phase 7 — AI Document Search

Status: Completed / MVP-ready.

### Phase 8 — MVP Hardening

Status: Completed / partially superseded by security review phases.

## Completed AI tool calling and orchestration track

### Phase 9 — AI Tool Calling and AI Orchestration

Status: Completed through 9P-H.

Completed sub-phases:

```text
9B Backend read-only AI tools
9C AI Assistant integration
9D AI Assistant frontend UX
9G Property and catalog read-only tools
9H Inventory and maintenance analytics tools
9I Scheduled maintenance, reservation and guest tools
9J Reservation supply and task tools
9K Purchase analytics tools
9L Document metadata and RAG health tools
9M File, image and dashboard analytics tools
9N Admin user, role and organization tools
9O AI chat session history tools
9P-A AI orchestration safe refactor
9P-B AI tool handler split
9P-C AI read-only domain service split
9P-C.1 AI tool package reorganization
9P-C.2 AI tool repository split
9P-D Tool/RAG fallback
9P-E LLM-driven planning
9P-F AI orchestration smoke tests and RAG diagnostics
9P-G AI orchestration observability and persisted debug traces
9P-H AI smoke test hardening and final fixes
```

### 9P-I — RAG retrieval tuning

Status: Conditional / optional / not started.

## Completed implementation queue after initial MVP

Phases 10A through 13A are completed and documented in their corresponding `.md` files.

## Completed Product Box Models phase

### Phase 14 — Product Box Models

Status: Completed through 14P.

Documentation: `71-product-box-models-14.md`

Completed subphases:

```text
14A Product Box Models backend foundation
14B Product Box Face Images
14C Angular Product Box CRUD
14D Three.js Product Box Viewer
14E Product Box 3D Textures architecture/design
14F Texture metadata + original upload
14G OpenCV perspective correction backend
14H Angular corner editor + processed texture preview
14I Accept/retry/delete texture workflow
14J Automatic contour detection and image enhancement
14K AI Texture Enhancement architecture/design
14L AI Texture metadata and backend provider abstraction
14M AI Texture enhancement backend
14N Angular AI enhanced preview and accept workflow
14N.1 Product Box OpenCV runtime controls and testing profile
14O Integration with Inventory/Purchases
14P AI awareness for Product Box Models
```

Product Box Models phase is complete for now.

## Completed organization administration and global UX polish phase

### Phase 15 — Organization Administration and Global UX Polish

Status: Completed through 15E.

Documentation: `93-organization-administration-global-ux-15.md`

Completed subphases:

```text
15A Organization logo backend + current organization header
15B Organization administration page
15C Organization switcher / multi-organization navigation
15C.1 Global SUPER_ADMIN organization navigation
15C.2 User organization memberships management
15D Icon-only action buttons with tooltips
15E TAMI branding and robot animation
```

Phase decisions:

- Organizations support logos.
- The active organization logo appears next to the selected organization name.
- A `SUPER_ADMIN` role administers all organizations.
- `SUPER_ADMIN` navigation is global across active organizations and does not depend on a membership row in every organization.
- Only `SUPER_ADMIN` users can assign users to other organizations and define their role in each organization.
- Organization `ADMINISTRATOR` users can edit only their current organization.
- Organization switching is secure and does not rely on a client-only organization id override.
- Icon-only action buttons use a shared reusable base for consistency, accessibility and tooltips.
- The sidebar shows `TAMI` instead of `AI Assistant`.
- TAMI robot identity is reusable in the sidebar and AI Assistant page.
- The robot animation in the AI session title starts when the typewriter response starts and stops when the typewriter response finishes.

## Future phases

```text
16 Reports
17 Notifications and reminders
18 Blueprint Analysis
```
