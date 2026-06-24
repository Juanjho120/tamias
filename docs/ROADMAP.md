# TAMIAS — Roadmap

This roadmap reflects the current state after AI tool calling/orchestration, S3/image hard-delete work, inventory brand/image work, AI debug traces, smoke-test hardening, Product Box Models through AI awareness, Phase 15 organization/global UX work, the AI read-only support decomposition and Payments through AI awareness.

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

Status: Completed through 15F.

Documentation:

- `93-organization-administration-global-ux-15.md`
- `105-ai-read-only-tool-support-decomposition-15f.md`

Completed subphases:

```text
15A Organization logo backend + current organization header
15B Organization administration page
15C Organization switcher / multi-organization navigation
15C.1 Global SUPER_ADMIN organization navigation
15C.2 User organization memberships management
15D Icon-only action buttons with tooltips
15E TAMI branding and robot animation
15F AI read-only tool support decomposition
```

## Completed payments phase

### Phase 16 — Payments

Status: Completed through 16E.

Documentation: `106-payments-16.md`

Purpose:

- Add a small organization-scoped payment registry for operational expenses and payments.
- Support payment categories as part of the catalog system.
- Support payment images/receipts stored privately in S3 with presigned URLs.
- Add a dedicated Angular payments screen.
- Add read-only TAMI awareness for payment summaries and searches.

Completed subphases:

```text
16A Payments backend foundation
16B Payment images with S3 hard delete
16C Payment categories in catalogs
16D Angular payments page
16E AI awareness for payments
```

Phase decisions:

- Payments are an internal operational registry, not a payment processor.
- Payments use soft delete.
- Payment images use hard delete from S3 and database.
- Payment categories are organization-scoped catalogs.
- TAMI payment tools are read-only and scoped to the selected organization.
- `SUPER_ADMIN` payment queries use the organization selected in the token, not a global cross-organization query.
- Payment AI awareness is implemented with a dedicated handler/service/repository and does not grow `AiReadOnlyToolSupport` again.

## Active / next implementation phase

### Phase 17 — Reports

Status: Planned / next.

Documentation: `107-reports-17.md`

## Future phases

```text
18 Notifications and reminders
19 Blueprint Analysis
```

Future phase documents:

```text
108-notifications-reminders-18.md
109-blueprint-analysis-19.md
```

The previous documents `94-reports-16.md`, `95-notifications-reminders-17.md` and `96-blueprint-analysis-18.md` are superseded because Payments was introduced as Phase 16 before Reports.
