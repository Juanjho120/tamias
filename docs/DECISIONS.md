# TAMIAS — Architecture and Technical Decisions

This document records important technical decisions for TAMIAS.

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

Rule:

> The backend resolves organization from authenticated user context. The frontend is not trusted as source of truth for organization isolation.

---

## 3. Backend framework

Decision:

```text
Use Java 21 + Spring Boot 3.
```

---

## 4. Frontend framework

Decision:

```text
Use Angular + TypeScript + Bootstrap.
```

---

## 5. User management

Decision:

```text
Keep user administration under /users and restrict it to administrators.
```

---

## 6. Self-service profile

Decision:

```text
Add /profile as a self-service screen for every authenticated user.
```

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

---

## 8. Inventory Items refactor

Decision:

```text
Replace the old materials catalog with Inventory Items.
```

Current model:

```text
inventory_items
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

Reports are now planned as Phase 15, after Product Box Models.

---

## 11. Tool Calling

Decision:

```text
Tool Calling is read-only, controlled and routed through backend-owned domain services/repositories.
```

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

---

## 13. Assistant display name

Decision:

```text
Use TAMI as the visible assistant name in the UI.
```

---

## 14. AI response animation

Decision:

```text
Use a frontend-only typewriter animation first.
```

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

Rules:

- `s3_key` remains the relative key used by AWS S3 operations.
- `s3_key` must include the organization id prefix.
- `filepath` stores the configured bucket plus the folder path without the filename.
- `filepath` must use the bucket configured by environment/properties.
- New uploads must use the organization/module/entity path strategy.

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
- New entity image tables must not add `deleted_at` or `deleted_by`.

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

---

## 18. Inventory item brand ownership

Decision:

```text
Brands belong directly to inventory_items, not purchase_items.
```

---

## 19. Purchase list images

Decision:

```text
Images for purchases are associated with purchase lists using purchase_list_id.
```

---

## 20. User last login

Decision:

```text
Track the last successful login timestamp on users.last_login.
```

---

## 21. Image upload modal UX consistency

Decision:

```text
All entity image upload modals should use the same UX baseline.
```

Rules:

- Show previews for selected images before upload.
- Provide a Clear button to reset selected files/previews.
- Align Clear and Upload buttons on the right side of the modal.
- Use the Bootstrap upload icon `bi bi-upload` for upload actions.
- Use the app's `ConfirmModalComponent` for delete confirmation.
- Do not use native browser dialogs such as `window.confirm`.
- Restrict image file pickers to JPG, PNG and WEBP.

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

---

## 23. AI image and inventory brand tools

Decision:

```text
Add read-only AI tools for image metadata and inventory brand queries.
```

---

## 24. AI image/file dashboard tools

Decision:

```text
Implement 13A cross-module AI image/file dashboard questions before deeper AI observability.
```

---

## 25. AI orchestration persisted debug traces

Decision:

```text
Persist one AI debug trace per assistant/TAMI message and expose it only to users with ai_chat_debug enabled.
```

---

## 26. AI debug traces are persisted by assistant message

Decision:

```text
Persist one debug trace per assistant/TAMI response in ai_chat_message_debugs.
```

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
- The minimal smoke set in `70-ai-smoke-test-hardening-final-fixes-9p-h.md` should be run after every future AI-related change.

---

## 28. Phase numbering after 13A

Decision:

```text
Do not reuse Phase 13A. Start Product Box Models as Phase 14, then split Reports, Notifications and Blueprint Analysis into separate later phases.
```

Current sequence:

```text
13A — AI image/file dashboard tools, completed
14 — Product Box Models
15 — Reports
16 — Notifications and reminders
17 — Blueprint Analysis
```

Reason:

- `13A` is already documented and completed.
- Reusing the same phase number would make the roadmap ambiguous.
- Reports and Notifications are different concerns and should not be implemented as one large mixed phase.
- Blueprint Analysis requires OCR/vision design and should stay separate.

---

## 29. Product Box Models architecture

Decision:

```text
Implement Product Box Models as metadata-driven rectangular boxes rendered dynamically in Angular with Three.js.
```

Rules:

- Backend stores metadata and S3 keys only.
- Backend does not render 3D.
- PostgreSQL must not store image binaries.
- The initial shape is a simple rectangular prism/box only.
- The frontend reconstructs the box using width, height, depth, unit and face image URLs.
- Do not generate `.glb`/`.gltf` files in the MVP.
- Product box images must use private S3 objects and presigned URLs.
- Product box face image deletes/replacements must delete S3 objects and must not leave orphan files.
- Product box face rows must follow hard-delete image policy and must not have `deleted_at`/`deleted_by`.
- Parent `product_box_models` may use soft delete as a business entity.
- Every query must be scoped by `organization_id` from authenticated user context.

Recommended S3 key:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/{filename}
```

Recommended filepath:

```text
{bucket}/{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}
```

Reason:

- Keeps the product box module aligned with existing S3 and multi-tenant decisions.
- Separates 3D package face images from normal inventory item images.
- Allows a safe incremental implementation: metadata first, images second, Angular CRUD third, Three.js viewer fourth.
