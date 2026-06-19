# TAMIAS — Architecture and Technical Decisions

This document records important technical decisions for TAMIAS.

---

## 1. Architecture style

Decision:

```text
Use Modular Monolith for the MVP.
```

Reason:

- Lower operational complexity.
- Faster development.
- Easier deployment.
- Easier testing.
- Enough for the expected early scale.
- Still allows clean separation by domain modules.

---

## 2. Multi-tenancy strategy

Decision:

```text
Shared database + shared schema + organization_id
```

Reason:

- Simpler MVP.
- Lower cost.
- Easier reporting.
- Easier deployment.
- Good fit for early SaaS stage.

Rule:

> The backend resolves organization from authenticated user context. The frontend is not trusted as source of truth for organization isolation.

---

## 3. Backend framework

Decision:

```text
Use Java 21 + Spring Boot 3.
```

Reason:

- Strong backend stack for portfolio.
- Good ecosystem for security, JPA, validation, scheduling, S3 and AI.
- Aligns with target professional experience.

---

## 4. Frontend framework

Decision:

```text
Use Angular + TypeScript + Bootstrap.
```

Reason:

- Good fit for admin dashboards.
- Strong typed frontend.
- Reactive Forms.
- Fast UI development with Bootstrap.
- Relevant professional stack.

---

## 5. User management

Decision:

```text
Keep user administration under /users and restrict it to administrators.
```

Reason:

- User creation, role assignment and status management are administrative operations.
- Backend must enforce this with authorization.
- Frontend menu visibility is only a UX convenience, not the security source of truth.

---

## 6. Self-service profile

Decision:

```text
Add /profile as a self-service screen for every authenticated user.
```

Reason:

- Non-admin users must be able to update their own basic data.
- Users must be able to change their own password.
- This avoids requiring an administrator for basic profile updates.

Allowed fields:

```text
firstName
lastName
password
```

Not allowed from profile:

```text
email
role
status
organization
```

---

## 7. Mandatory temporary password change

Decision:

```text
New users created by an administrator must change their temporary password on first login.
```

Implementation:

```text
users.password_change_required
```

Rules:

- Existing users default to `false`.
- New admin-created users are created with `true`.
- Login response includes `passwordChangeRequired`.
- Frontend redirects users with `passwordChangeRequired = true` to `/profile`.
- User cannot continue using the rest of the app until password is changed.
- Successful password change sets `passwordChangeRequired = false`.

Reason:

- Admin-created passwords are temporary.
- Users should take ownership of their password.
- Reduces risk of shared or reused temporary credentials.

---

## 8. Inventory Items refactor

Decision:

```text
Replace the old materials catalog with Inventory Items.
```

Reason:

The old `materials` concept was too narrow. TAMIAS needs one shared operational item catalog that can support:

- maintenance usage,
- purchase lists,
- reservation supplies,
- future inventory reporting,
- future AI tool calling.

Current model:

```text
inventory_items
```

Business label:

```text
Spanish: Insumos y materiales
English: Inventory Items
```

Operational availability is controlled by:

```text
available_for_maintenance
available_for_reservations
available_for_purchases
```

---

## 9. Reservation supplies

Decision:

```text
Use reservation_supplies for items delivered to guests during reservations.
```

Reason:

- Supplies are operationally different from reservation header data.
- Supplies may be added/edited after reservation creation.
- Supplies should be reportable.
- Supplies should be queryable by future AI tools.

Frontend decision:

```text
Use a separate Supplies modal from the Reservations table.
```

---

## 10. Reports

Decision:

```text
Do not implement JasperReports in the MVP.
```

Reason:

- More valuable to first complete stable operational flows.
- Reports can build on existing clean data later.
- Dashboard analytics should come before full PDF reporting.

---

## 11. Tool Calling

Decision:

```text
Tool Calling is read-only, controlled and routed through backend-owned domain services/repositories.
```

Reason:

- Tool calling should run over stable domain services.
- Read-only tools should come first.
- No SQL should be exposed directly to the model.
- The model must not execute arbitrary actions or arbitrary queries.

Rules:

- No free SQL execution.
- Tools must enforce organization isolation.
- Tools must expose controlled domain queries.
- Administrative tools must check authorization.
- Guardrails must block write/action requests.

---

## 12. AI chat ownership

Decision:

```text
AI chat sessions and messages are private to the authenticated owner user by default.
```

Rules:

- AI chat list endpoints must only return sessions created by the authenticated user.
- AI chat message endpoints must verify that the session belongs to the authenticated user.
- AI history tools must only read the authenticated user's own sessions/messages.
- Organization isolation is still mandatory, but it is not sufficient by itself for chat privacy.
- Administrators do not automatically see every user's chat history in the normal AI Assistant UI/tools.

Reason:

- Chat history can contain sensitive operational details.
- A restricted user must not see questions/answers from another user.
- The backend, not the frontend, must enforce ownership.

---

## 13. Assistant display name

Decision:

```text
Use TAMI as the visible assistant name in the UI.
```

Reason:

- TAMI matches the TAMIAS product identity better than a generic `ASISTENTE` label.
- This is a UI display decision, not a backend model identity requirement.

---

## 14. AI response animation

Decision:

```text
Use a frontend-only typewriter animation first.
```

Reason:

- The backend can keep returning complete answers.
- Simulated typewriter animation is simpler than SSE/WebSocket streaming.
- It improves UX without changing the AI orchestration contract.

---

## 15. S3 key strategy

Decision:

```text
Store files by organization, module and entity id instead of year/month folders.
```

Target structure:

```text
{organizationId}/properties/{propertyId}/
{organizationId}/reservations/{reservationId}/
{organizationId}/catalogs/inventory_items/{inventoryItemId}/
{organizationId}/maintenance/{maintenanceRecordId}/
{organizationId}/purchases/{purchaseListId}/
{organizationId}/documents/
{organizationId}/documents/{propertyId}/
```

Bucket example:

```text
tamias-dev-files/
  {organizationId}/
    properties/{propertyId}/Image1.jpg
    reservations/{reservationId}/Image1.jpg
    catalogs/inventory_items/{inventoryItemId}/Image1.jpg
    maintenance/{maintenanceRecordId}/Image1.jpg
    purchases/{purchaseListId}/Image1.jpg
    documents/Document1.pdf
    documents/{propertyId}/Document1.pdf
```

Rules:

- `s3_key` remains the relative key used by AWS S3 operations.
- `s3_key` must include the organization id prefix.
- `filepath` stores the configured bucket plus the folder path without the filename.
- `filepath` must use the bucket configured by environment/properties.
- New uploads must use the organization/module/entity path strategy.

Examples:

```text
s3_key: {organizationId}/properties/{propertyId}/uuid_Image1.jpg
filepath: tamias-dev-files/{organizationId}/properties/{propertyId}
s3_key: {organizationId}/documents/{propertyId}/uuid_Document1.pdf
filepath: tamias-dev-files/{organizationId}/documents/{propertyId}
```

Reason:

- Keeps each organization's files grouped together in the bucket.
- Improves multi-tenant operational visibility.
- Makes manual S3 inspection easier.
- Avoids mixing unrelated module files in year/month folders.
- Keeps future cleanup by organization/entity simpler.

---

## 16. Entity images hard delete

Decision:

```text
Entity image relationship tables do not use soft delete.
```

Applies to:

```text
property_images
maintenance_record_images
reservation_images
purchase_images
inventory_item_images
```

Rules:

- Deleting an image deletes the S3 object first.
- Deleting an image physically deletes the database row after successful storage delete.
- If storage/S3 deletion fails, the DB row must not be deleted.
- Schema cleanup should remove `deleted_at` from image relationship tables where present.
- New entity image tables must not add `deleted_at` or `deleted_by`.
- No orphan image records or orphan S3 objects should remain after a successful delete.

---

## 17. RAG documents hard delete

Decision:

```text
RAG documents do not use soft delete.
```

Deletion flow:

```text
1. Delete the original document from S3.
2. Delete vectors from Chroma.
3. Delete document_chunks rows.
4. Delete documents row.
```

Rules:

- Abort the delete transaction if S3 cleanup fails.
- Abort the delete transaction if Chroma cleanup fails.
- Do not leave stale vectors or stale document chunks after a successful delete.
- `documents.deleted_at` and `documents.deleted_by` must not remain in the schema.
- `DocumentStatus` must not expose `DELETED`; deleted documents are physically removed.
- AI metadata queries must not reference `d.deleted_at IS NULL` for documents.

---

## 18. Inventory item brand ownership

Decision:

```text
Brands belong directly to inventory_items, not purchase_items.
```

Rules:

- Add `inventory_items.brand_id`.
- Remove `purchase_items.brand_id`.
- Purchase items should derive/display brand from their referenced inventory item.
- Inventory item search labels should show `{item name} - {brand}` when brand exists.
- Inventory item catalog tables should keep item name and brand in separate columns.
- Modals/selectors that need disambiguation should show item + brand.

Reason:

- Brand describes the catalog item, not only one purchase line.
- This avoids duplicated brand selection across purchase lists.
- It makes searches and AI answers more consistent.

---

## 19. Purchase list images

Decision:

```text
Images for purchases are associated with purchase lists using purchase_list_id.
```

Reason:

- The existing module concept is Purchase Lists.
- The image relationship table should follow the actual parent entity.
- Avoid inventing a generic `purchase_id` when the domain entity is `purchase_list`.

---

## 20. User last login

Decision:

```text
Track the last successful login timestamp on users.last_login.
```

Rules:

- Update `users.last_login` only after successful authentication.
- Use the same timestamp style as the existing audit fields.
- Surface the value in access summaries where useful.

---

## 21. Image upload modal UX consistency

Decision:

```text
All entity image upload modals should use the same UX baseline.
```

Applies to:

```text
properties
maintenance records
inventory items
purchase lists
reservations
```

Rules:

- Show previews for selected images before upload.
- Provide a Clear button to reset selected files/previews.
- Align Clear and Upload buttons on the right side of the modal.
- Use the Bootstrap upload icon `bi bi-upload` for upload actions.
- Use the app's `ConfirmModalComponent` for delete confirmation.
- Do not use native browser dialogs such as `window.confirm`.
- Restrict image file pickers to JPG, PNG and WEBP:

```text
accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
```

Reason:

- Keeps UX consistent across modules.
- Avoids ugly/native browser confirmations.
- Prevents accidental non-image file selection where possible.
- Matches the pattern already established by properties and maintenance images.

---

## 22. Frontend i18n strategy for image modules

Decision:

```text
Use static JSON translation files for image modal labels and messages.
```

Translation files:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

Rules:

- Do not create feature-specific translation services.
- Do not register feature translations dynamically from code.
- Do not hardcode Spanish or English labels/messages in templates/components.
- Use the existing `LanguageService` and `TranslateService` flow.

Image modal translation namespaces:

```text
properties.images
maintenance.images
catalogs.items.inventoryItems.images
purchases.images
reservations.images
```

Reason:

- The app already has a working i18n architecture.
- Static JSON keeps translations predictable and easy to review.
- Feature code should not own translation registration.

---

## 23. AI image and inventory brand tools

Decision:

```text
Add read-only AI tools for image metadata and inventory brand queries.
```

Tools:

```text
images.getReservationImages
images.getInventoryItemImages
images.getPurchaseListImages
inventory.getItemsByBrand
```

Rules:

- Tools must remain read-only.
- Tools must enforce `organization_id` filtering.
- Tools must not expose presigned image URLs unless a future requirement explicitly asks for it.
- Tools must not delete, generate or edit images.
- Image tools should prefer structured database metadata over RAG.
- Inventory tools should include item brand where relevant.
- Questions about `productos` should route to inventory when the context is inventory, brand, usage or generic product catalog.
- Questions clearly about purchased products should still route to purchase analytics.

Reason:

- TAMI should answer operational questions from structured TAMIAS data before falling back to documents.
- Image metadata is now distributed across several modules and needs a controlled read-only access layer.
- Brand ownership moved to inventory items, so AI answers should reflect that domain model.

---

## 24. AI image/file dashboard tools

Decision:

```text
Implement 13A cross-module AI image/file dashboard questions before resuming 9P-G.
```

Implemented examples:

```text
¿Cuántas imágenes tengo por módulo?
¿Qué entidades tienen más imágenes?
¿Qué entidades no tienen imágenes?
¿Qué imágenes se subieron recientemente?
¿Qué archivos ocupan más espacio?
```

Implemented tools:

```text
files.getImageDashboardSummary
files.getRecentUploads
files.getLargestFiles
files.getEntitiesWithoutImages
files.getEntitiesWithMostImages
```

Implementation decision:

- Extend the existing 9M `FileImageDashboardToolHandler`, `FileImageReadOnlyToolService` and `FileImageToolRepository`.
- Do not create a parallel dashboard package for 13A.
- Keep module-specific image tools in `EntityImageToolHandler`.
- Include 12B/12C/12D image tables in file metadata and storage summary queries.

Rules:

- Reuse or extend existing 9M file/image/dashboard tools when possible.
- Keep tools read-only.
- Keep all queries organization-scoped.
- Do not add cleanup/repair actions in 13A.

Reason:

- 12B–12E added new image tables and module-specific image tools.
- 13A should provide cross-module visibility before deeper AI observability work.
- This creates a cleaner checkpoint before 9P-G/H/I.

---

## 25. AI orchestration persisted debug traces

Decision:

```text
Persist one AI debug trace per assistant/TAMI message and expose it only to users with ai_chat_debug enabled.
```

Table:

```text
ai_chat_message_debugs
```

User flag:

```text
users.ai_chat_debug
```

Rules:

- Store one consolidated debug row for each assistant/TAMI response message.
- `ai_chat_message_debugs.ai_chat_message_id` must reference the assistant `ai_chat_messages.id`.
- Store selected handler, primary tool, all tools, sanitized params, RAG usage and answer source.
- `answerSource` describes how the final answer was produced; it must not be overloaded with tool names.
- Tool names belong in `tool_name` and `tool_names`.
- Persist debug traces regardless of whether the user can see them.
- Include debug in API responses only when the authenticated chat owner has `users.ai_chat_debug = true`.
- Keep debug hidden by default.
- Do not expose debug traces across users or chats.
- Do not store secrets, raw stack traces, JWTs, S3 credentials, presigned URLs or arbitrary SQL in debug params/errors.

Recommended `answerSource` values:

```text
BACKEND_DIRECT
LLM_COMPOSED
RAG
TOOLS_AND_RAG
NO_MATCH
ERROR
```

Reason:

- Recent AI tool routing fixes showed that incorrect answers can come from planner, handler, tool parameter extraction, RAG fallback or LLM composition.
- Persisted traces make debugging reproducible instead of relying only on screenshots or manual guesses.
- A per-message trace keeps chat history and troubleshooting linked.
- The `ai_chat_debug` flag protects normal users from noisy diagnostic output while allowing controlled developer/admin troubleshooting.

---

## 26. AI debug traces are persisted by assistant message

Decision:

```text
Persist one debug trace per assistant/TAMI response in ai_chat_message_debugs.
```

Rules:

- The trace references `ai_chat_messages.id` for the assistant response.
- The trace stores handler, primary tool, all tools used, params, RAG usage and answer source.
- Traces are always persisted for observability.
- Traces are only exposed in API responses when `users.ai_chat_debug = true` for the current user.
- `answer_source` identifies the final answer path; tools remain in `tool_name`/`tool_names`.

Reason:

This keeps normal chat responses clean while making routing/tool/RAG/composition issues auditable during development and QA.

---

## 27. AI smoke-test hardening closes routing/formatting regressions before RAG tuning

Decision:

```text
Complete 9P-H after targeted smoke-test fixes and keep 9P-I conditional.
```

Rules:

- Smoke-test failures should be categorized before changing RAG behavior.
- Structured TAMIAS questions should first be fixed in routing, parameter extraction, tool queries or answer formatting.
- RAG tuning must not be used to compensate for broken structured-tool routing.
- Guardrail/write prompts must route to read-only denial and must not fall back to RAG.
- Empty operational tool results should return terminal domain-specific no-result messages when RAG cannot add value.
- Image/file dashboard prompts should return prompt-specific formats, not always the full dashboard summary.
- Purchases, inventory, maintenance, scheduled maintenance, tasks, documents and file/image dashboard tools must keep organization-scoped queries and backend-direct answers where applicable.
- The minimal smoke set in `70-ai-smoke-test-hardening-final-fixes-9p-h.md` should be run after every future AI-related change.

Fix areas completed during 9P-H:

- Read-only guard routing for write/action prompts.
- Maintenance item date formatting.
- Direct answer for top maintenance cost by property.
- Grouped overdue scheduled maintenance output.
- Terminal no-result messages for scheduled maintenance, maintenance images and document metadata cases.
- Completed/assigned task grouping, including reservation primary guest display.
- Purchase last-purchased search by brand.
- Most-purchased item/product routing to purchase analytics.
- Correct failed-document routing.
- Prompt-specific image dashboard summaries, storage summaries, file name lists and largest-file grouping.

Reason:

- The 9P-H smoke run showed that most issues were routing, extraction, formatting or terminal no-result behavior, not RAG retrieval quality.
- Closing 9P-H creates a stable checkpoint before deciding whether 9P-I is needed.
- Keeping 9P-I conditional prevents unnecessary RAG tuning while structured AI tools are already answering the relevant operational questions.
