# TAMIAS — Roadmap

This roadmap reflects the current state after the AI tool calling and orchestration phases, the completed implementation queue for UX, security, S3 storage, image handling, inventory brands, AI image/brand tools, AI debug traces and AI smoke-test hardening.

---

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

---

## Completed implementation queue before deeper AI observability

These phases were implemented before and during the deeper AI observability track because they changed security, file ownership, storage paths, documents, images, inventory items, purchases, reservations and AI chat behavior.

### Phase 10A — Chat ownership security + quick AI UI fixes + last login

Status: Completed.

Documentation: `56-chat-ownership-quick-ai-ui-fixes-10a.md`

Goals:

- Restrict AI chat sessions and messages to the authenticated owner user.
- Ensure AI history tools do not expose another user's chat history.
- Add `users.last_login` and update it after successful login.
- Send AI message with Enter.
- Insert newline with Shift + Enter.
- Rename visible assistant label from `ASISTENTE` to `TAMI`.
- Auto-collapse mobile sidebar after selecting a route.

### Phase 10B — Typewriter animation for TAMI responses

Status: Completed.

Documentation: `57-ai-typewriter-response-10b.md`

Goals:

- Keep backend response complete/non-streaming.
- Simulate typewriter animation in Angular.
- Preserve sources, tool evidence and grounded status.

### Phase 11A — S3 key strategy + filepath fields

Status: Completed.

Documentation: `58-s3-key-strategy-filepath-fields-11a.md`

Goals:

- Replace year/month upload paths with module/entity paths.
- Add `filepath` to document and image relationship tables.
- Store full bucket + folder path in `filepath`, without filename.
- Keep `organizationId` as the first level inside the bucket.

### Phase 11B — Hard delete policy for entity images

Status: Completed.

Documentation: `59-hard-delete-entity-images-11b.md`

Goals:

- Remove soft-delete behavior from image relationship tables.
- Clean schema by removing `deleted_at` from image tables where present.
- Delete S3 object and database row when an image is removed.

### Phase 11C — Hard delete policy for RAG documents

Status: Completed.

Documentation: `60-hard-delete-rag-documents-11c.md`

Goals:

- Remove soft-delete behavior from RAG documents.
- Delete S3 object, Chroma vectors, document chunks and document row.
- Abort deletion if S3 or Chroma cleanup fails.

### Phase 11C.1 — Remove document soft-delete leftovers

Status: Completed.

Documentation: `65-remove-document-soft-delete-leftovers-11c1.md`

Goals:

- Remove `documents.deleted_at` and `documents.deleted_by`.
- Remove `Document.deletedAt` and `Document.deletedBy`.
- Remove `DELETED` from `DocumentStatus`.
- Remove stale `d.deleted_at IS NULL` predicates from AI metadata queries.
- Remove stale image soft-delete predicates against image tables already migrated to hard delete.

### Phase 12A — Associate brands directly with inventory items

Status: Completed.

Documentation: `61-inventory-item-brand-association-12a.md`

Goals:

- Add brand association to `inventory_items`.
- Remove `brand_id` from `purchase_items`.
- Display searchable inventory items as `{item name} - {brand}`.
- Keep inventory item catalog table name and brand in separate columns.
- Show item + brand in selectors/modals where disambiguation is needed.

### Phase 12B — Inventory item images

Status: Completed.

Documentation: `62-inventory-item-images-12b.md`

Goals:

- Add `inventory_item_images`.
- Add image upload/delete UI to Inventory Items catalog.
- Use hard delete and S3 key strategy with organization prefix.
- Use the same image modal pattern as properties and maintenance.
- Use static i18n files for all modal labels/messages.

### Phase 12C — Purchase list images

Status: Completed.

Documentation: `63-purchase-list-images-12c.md`

Goals:

- Add `purchase_images` with `purchase_list_id`.
- Add image upload/delete UI to Purchase Lists.
- Use hard delete and S3 key strategy with organization prefix.
- Use the same image modal pattern as properties and maintenance.
- Use static i18n files for all modal labels/messages.

### Phase 12D — Reservation images

Status: Completed.

Documentation: `64-reservation-images-12d.md`

Goals:

- Add `reservation_images` with `reservation_id`.
- Add image upload/delete UI to Reservations.
- Use hard delete and S3 key strategy with organization prefix.
- Use the same image modal pattern as properties and maintenance.
- Use static i18n files for all modal labels/messages.

### Phase 12E — AI image and inventory brand tools

Status: Completed.

Documentation: `66-ai-image-brand-tools-12e.md`

Goals:

- Add read-only AI tools for reservation images, inventory item images and purchase list images.
- Update inventory tools to include item brand in relevant answers.
- Add tools to query inventory items by brand.
- Route `productos` questions to inventory when the user is asking about inventory, brands or usage rather than purchase analytics.
- Avoid RAG fallback for structured inventory/image questions when system tools can answer.

### Phase 12 closure — Images, inventory brands and AI tools

Status: Completed.

Documentation: `67-phase-12-closure.md`

Goals:

- Record final hard-delete image policy.
- Record final S3/filepath policy.
- Record final image modal UX and i18n policy.
- Record final AI tool evidence expectations for image, brand and inventory questions.

### Phase 13A — AI image/file dashboard tools

Status: Completed.

Documentation: `68-ai-image-file-dashboard-tools-13a.md`

Goals:

- Add cross-module AI tools for image/file dashboard questions.
- Answer image counts by module.
- Answer entities with most images.
- Answer entities without images.
- Answer recent uploads.
- Answer largest files / storage summaries from metadata.
- Reuse or extend existing 9M file/image/dashboard tools when possible.
- Keep all tools read-only and organization-scoped.

---

## Latest AI observability and hardening phases

### 9P-G — AI orchestration observability and persisted debug traces

Status: Completed.

Documentation: `69-ai-orchestration-observability-debug-traces-9p-g.md`

Goals:

- Persist one debug trace row per assistant/TAMI response.
- Add `ai_chat_message_debugs` referencing the assistant `ai_chat_messages.id`.
- Add `users.ai_chat_debug` to control whether debug is included in API responses.
- Capture selected handler, primary tool, all tools, sanitized params, RAG usage and answer source.
- Keep `answerSource` separate from tool names.
- Show debug only to the authenticated chat owner when `ai_chat_debug = true`.
- Keep normal TAMI answers unchanged for users without debug enabled.

### 9P-H — Smoke test hardening and final fixes

Status: Completed.

Documentation: `70-ai-smoke-test-hardening-final-fixes-9p-h.md`

Goals:

- Run the AI smoke-test matrix after persisted debug traces.
- Validate guardrails, routing, parameter extraction, formatting, RAG fallback and debug persistence.
- Fix regressions found during smoke testing with targeted backend AI changes.
- Keep the AI assistant read-only.
- Keep tool-first behavior for structured TAMIAS data.
- Keep RAG for document-content questions.
- Leave `9P-I` conditional instead of starting RAG tuning automatically.

Fix areas covered:

- Read-only guard routing for write/action prompts.
- Maintenance date formatting and cost summary formatting.
- Scheduled maintenance grouping and empty-result messages.
- Maintenance images empty-result behavior.
- Task grouping and assigned-task primary guest display.
- Purchase last-purchased searches by brand.
- Routing for most-purchased item/product prompts.
- Document metadata routing for failed vs processed-not-indexed documents.
- Image dashboard prompt-specific formats.
- Largest files and file-name listing formats.

### 9P-I — RAG retrieval tuning

Status: Conditional / not started.

Goals:

- Tune threshold, topK, metadata filters, property-aware retrieval and chunk quality only if PDFs/documents still show retrieval issues.
- Do not start this phase for structured-tool routing or formatting problems.

---

## Future phases

### Reports, notifications and advanced AI

Status: Future.

Possible features:

- JasperReports PDF reports.
- Email notifications.
- Advanced reminders.
- Blueprint analysis with OCR and vision models.
- AI agents by business domain.
- Formal inventory stock control.
- Platform integrations.
- Billing and subscriptions.
