# TAMIAS — Roadmap

This roadmap reflects the current state after AI tool calling/orchestration, S3/image hard-delete work, inventory brand/image work, AI debug traces, smoke-test hardening and Product Box Models through the Three.js viewer texture crop/fit phase.

## Completed foundation

### Phase 0 — Project Setup

Status: Completed.

- Repository and documentation.
- Backend and frontend foundations.
- Local development setup.

### Phase 1 — Security and SaaS Foundation

Status: Completed / MVP-ready.

- Organizations.
- Users.
- Roles.
- Login.
- JWT.
- Protected routes.
- Current user context.
- Organization-based data isolation.
- User management for administrators.
- Self-service profile.
- Mandatory password change.

### Phase 2 — Properties and Catalogs

Status: Completed / MVP-ready.

- Property management.
- Property images.
- Maintenance categories.
- Maintenance types.
- Maintenance people.
- Platforms.
- Suppliers.
- Cities.
- Brands.
- Task templates.
- Inventory Items.

### Phase 3 — Maintenance

Status: Completed / MVP-ready.

- Maintenance records.
- Responsible people.
- Maintenance record items.
- Images.
- Costs.
- Scheduled maintenance.
- Rescheduling.
- Cancellation.
- History.
- Dashboard/calendar integration.

### Phase 4 — Reservations and Tasks

Status: Completed / MVP-ready.

- Reservations.
- Guests.
- Reservation supplies.
- Supplies modal.
- Reservation calendar/dashboard integration.
- Task lists.
- Checklists.
- Task completion.
- Related task lists modal.

### Phase 5 — Purchase Lists

Status: Completed / MVP-ready.

- Purchase lists.
- Purchase items.
- Suppliers.
- Cities.
- Brands.
- Inventory Items.
- Estimated prices.
- Purchased status.
- Items modal.
- Purchase list form item editing.

### Phase 6 — Documents

Status: Completed / MVP-ready.

- Document upload.
- AWS S3 storage.
- Secure file URLs.
- Document types.
- Processing status.
- Text extraction.
- Chunking.

### Phase 7 — AI Document Search

Status: Completed / MVP-ready.

- Embeddings.
- Chroma vector store.
- RAG search.
- AI answers with sources.
- AI chat sessions.
- Basic quality improvements.

### Phase 8 — MVP Hardening

Status: Completed / partially superseded by security review phases.

- Role-based access review.
- Multi-tenant filtering review.
- Backend and frontend hardening.
- Error handling consistency.

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

Sub-phases:

```text
14A Product Box Models backend foundation — Completed
14B Product Box Face Images — Completed
14C Angular Product Box CRUD — Completed
14D Three.js Product Box Viewer — Completed
14D.1 Auto texture crop and fit — Completed
14E Integration with Inventory/Purchases — Next
14F AI awareness for Product Box Models
```

### Phase 14A — Product Box Models backend foundation

Status: Completed.

Documentation: `72-product-box-models-backend-foundation-14a.md`

Implemented:

- Backend metadata CRUD only.
- Flyway migration for `product_box_models`.
- Entity, repository, DTOs, mapper, service and controller.
- Organization scoping.
- Optional inventory item / purchase item association.
- Soft delete for parent models.
- No face images, no S3, no Angular and no Three.js yet.

### Phase 14B — Product Box Face Images

Status: Completed.

Documentation: `76-product-box-face-images-14b.md`

Implemented:

- Flyway migration for `product_box_model_faces`.
- Upload/replace/delete one face image per box face.
- Private S3 storage using organization-first keys.
- Hard delete face rows and S3 objects.
- Presigned URLs in model detail responses.
- Parent model delete removes face S3 objects and face rows before soft-deleting the parent.
- No Angular CRUD and no Three.js yet.

### Phase 14C — Angular Product Box CRUD

Status: Completed.

Documentation: `77-product-box-angular-crud-14c.md`

Implemented:

- Angular list/form UI for Product Box Models.
- Face upload controls for front/back/left/right/top/bottom.
- Static i18n keys under `productBoxModels`.
- No Three.js viewer yet.

### Phase 14D — Three.js Product Box Viewer

Status: Completed.

Documentation: `78-threejs-product-box-viewer-14d.md`

Implemented:

- Reusable Angular Three.js viewer component.
- Modal-based 3D preview from Product Box Models list.
- Texture per face using presigned URLs.
- Placeholder materials for missing faces.
- Orbit controls for rotate/zoom.
- Dynamic Three.js imports and cleanup of WebGL resources.

### Phase 14D.1 — Auto texture crop and fit

Status: Completed.

Documentation: `79-product-box-texture-crop-fit-14d1.md`

Implemented:

- Canvas preprocessing before creating Three.js textures.
- Alpha-based crop for transparent PNG/WebP images.
- Border-background crop for non-transparent images.
- Per-face aspect ratio fitting.
- Original S3 images remain unchanged.

## Future phases after Product Box Models

### Phase 14E — Integration with Inventory/Purchases

Status: Planned / next.

Expected scope:

- Show Product Box Models from inventory item and purchase item contexts.
- Allow creating a model from an existing item.
- Show indicator when an item already has a 3D box model.

### Phase 14F — AI awareness for Product Box Models

Status: Planned.

Expected scope:

- Let TAMI answer metadata-only questions about Product Box Models.
- No image interpretation.
- No OCR/vision.

### Phase 15 — Reports

Status: Future.

Documentation: `73-reports-15.md`

Candidate scope:

- Operational reports.
- Maintenance cost reports.
- Purchase cost reports.
- Reservation summaries.
- PDF generation, likely with JasperReports after a dedicated design pass.

### Phase 16 — Notifications and reminders

Status: Future.

Documentation: `74-notifications-reminders-16.md`

Candidate scope:

- Email notifications.
- Scheduled maintenance reminders.
- Reservation check-in/check-out reminders.
- Task due reminders.
- Notification preferences.

### Phase 17 — Blueprint Analysis

Status: Future.

Documentation: `75-blueprint-analysis-17.md`

Candidate scope:

- Blueprint/floor plan upload and analysis.
- OCR/vision-assisted extraction only after a dedicated design phase.
- Measurement search and AI metadata awareness when reliable enough.

### Later candidates

- AI agents by business domain.
- Formal inventory stock control.
- Platform integrations.
- Billing and subscriptions.
