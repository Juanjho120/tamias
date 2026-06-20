# TAMIAS — Roadmap

This roadmap reflects the current state after AI tool calling/orchestration, S3/image hard-delete work, inventory brand/image work, AI debug traces, smoke-test hardening and Product Box Models through the Product Box 3D Textures design phase.

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

Goals:

- Tune threshold, topK, metadata filters, property-aware retrieval and chunk quality only if PDFs/documents still show retrieval issues.
- Do not start this phase for structured-tool routing, parameter extraction or formatting problems.
- Keep 9P-I optional for now.

## Completed implementation queue after initial MVP

### Phase 10A — Chat ownership security + quick AI UI fixes + last login
Status: Completed.
Documentation: `56-chat-ownership-quick-ai-ui-fixes-10a.md`

### Phase 10B — Typewriter animation for TAMI responses
Status: Completed.
Documentation: `57-ai-typewriter-response-10b.md`

### Phase 11A — S3 key strategy + filepath fields
Status: Completed.
Documentation: `58-s3-key-strategy-filepath-fields-11a.md`

### Phase 11B — Hard delete policy for entity images
Status: Completed.
Documentation: `59-hard-delete-entity-images-11b.md`

### Phase 11C — Hard delete policy for RAG documents
Status: Completed.
Documentation: `60-hard-delete-rag-documents-11c.md`

### Phase 11C.1 — Remove document soft-delete leftovers
Status: Completed.
Documentation: `65-remove-document-soft-delete-leftovers-11c1.md`

### Phase 12A — Associate brands directly with inventory items
Status: Completed.
Documentation: `61-inventory-item-brand-association-12a.md`

### Phase 12B — Inventory item images
Status: Completed.
Documentation: `62-inventory-item-images-12b.md`

### Phase 12C — Purchase list images
Status: Completed.
Documentation: `63-purchase-list-images-12c.md`

### Phase 12D — Reservation images
Status: Completed.
Documentation: `64-reservation-images-12d.md`

### Phase 12E — AI image and inventory brand tools
Status: Completed.
Documentation: `66-ai-image-brand-tools-12e.md`

### Phase 12 closure — Images, inventory brands and AI tools
Status: Completed.
Documentation: `67-phase-12-closure.md`

### Phase 13A — AI image/file dashboard tools
Status: Completed.
Documentation: `68-ai-image-file-dashboard-tools-13a.md`

### 9P-G — AI orchestration observability and persisted debug traces
Status: Completed.
Documentation: `69-ai-orchestration-observability-debug-traces-9p-g.md`

### 9P-H — Smoke test hardening and final fixes
Status: Completed.
Documentation: `70-ai-smoke-test-hardening-final-fixes-9p-h.md`

## Current implementation queue

### Phase 14 — Product Box Models
Status: In progress.
Documentation: `71-product-box-models-14.md`

Purpose:

- Add a module to register simple rectangular product box/package models.
- Store dimensions and metadata in PostgreSQL.
- Store face images in private S3 using the existing organization-first key strategy.
- Render the box dynamically in Angular using Three.js.
- Process phone photos into clean face textures using OpenCV Java.

Sub-phases:

```text
14A Product Box Models backend foundation — Completed
14B Product Box Face Images — Completed
14C Angular Product Box CRUD — Completed
14D Three.js Product Box Viewer — Completed
14E Product Box 3D Textures architecture/design — Completed / design ready
14F Texture metadata + original upload — Next
14G OpenCV perspective correction backend — Planned
14H Angular corner editor + processed texture preview — Planned
14I Accept/retry/delete texture workflow — Planned
14J Automatic contour detection and image enhancement — Planned
14K Integration with Inventory/Purchases — Planned
14L AI awareness for Product Box Models — Planned
```

### Phase 14A — Product Box Models backend foundation
Status: Completed.
Documentation: `72-product-box-models-backend-foundation-14a.md`

### Phase 14B — Product Box Face Images
Status: Completed.
Documentation: `76-product-box-face-images-14b.md`

### Phase 14C — Angular Product Box CRUD
Status: Completed.
Documentation: `77-product-box-angular-crud-14c.md`

### Phase 14D — Three.js Product Box Viewer
Status: Completed.
Documentation: `78-threejs-product-box-viewer-14d.md`

### Phase 14E — Product Box 3D Textures architecture/design
Status: Completed / design ready.
Documentation: `79-product-box-3d-textures-14e.md`

Defined:

- Non-generative OpenCV-based texture pipeline.
- Original, processed and accepted texture lifecycle.
- Manual four-corner perspective correction flow.
- Automatic contour detection as a later subphase.
- Basic image enhancement as a later subphase.
- S3 paths and hard-delete rules for original/processed/accepted images.

### Phase 14F — Texture metadata + original upload
Status: Next.

Expected scope:

- Extend `product_box_model_faces` with texture processing metadata.
- Upload original phone photos to S3.
- Return original/processed/accepted presigned URLs where applicable.
- No OpenCV processing yet.

### Phase 14G — OpenCV perspective correction backend
Status: Planned.

Expected scope:

- Add OpenCV Java dependency.
- Process four manually selected points.
- Generate a rectangular texture matching the real face aspect ratio.
- Store processed texture in S3.

### Phase 14H — Angular corner editor + processed texture preview
Status: Planned.

Expected scope:

- Four-corner editor overlay.
- Drag points and send coordinates to backend.
- Preview processed texture.

### Phase 14I — Accept/retry/delete texture workflow
Status: Planned.

Expected scope:

- Accept processed texture as active face texture.
- Retry processing.
- Delete original/processed/accepted images safely.

### Phase 14J — Automatic contour detection and image enhancement
Status: Planned.

Expected scope:

- Auto-detect rectangular contour when possible.
- Prefill editor points.
- Allow manual correction.
- Apply conservative lighting/contrast enhancement.

### Phase 14K — Integration with Inventory/Purchases
Status: Planned.

Expected scope:

- Show Product Box Models from inventory item and purchase item contexts.
- Allow creating a model from an existing item.
- Show indicator when an item already has a 3D box model.

### Phase 14L — AI awareness for Product Box Models
Status: Planned.

Expected scope:

- Let TAMI answer metadata-only questions about Product Box Models.
- No image interpretation.
- No OCR/vision.

## Future phases after Product Box Models

### Phase 15 — Reports
Status: Future.
Documentation: `73-reports-15.md`

### Phase 16 — Notifications and reminders
Status: Future.
Documentation: `74-notifications-reminders-16.md`

### Phase 17 — Blueprint Analysis
Status: Future.
Documentation: `75-blueprint-analysis-17.md`

### Later candidates

- AI agents by business domain.
- Formal inventory stock control.
- Platform integrations.
- Billing and subscriptions.
