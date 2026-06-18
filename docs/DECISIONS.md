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
    properties/
      {propertyId}/
        Image1.jpg
        Image2.jpg
    reservations/
      {reservationId}/
        Image1.jpg
    catalogs/
      inventory_items/
        {inventoryItemId}/
          Image1.jpg
    maintenance/
      {maintenanceRecordId}/
        Image1.jpg
    purchases/
      {purchaseListId}/
        Image1.jpg
    documents/
      Document1.pdf
      Document2.pdf
      {propertyId}/
        Document1.pdf
```

Rules:

- `s3_key` remains the relative key used by AWS S3 operations.
- `s3_key` must include the organization id prefix.
- `filepath` stores the configured bucket plus the folder path without the filename.
- `filepath` must use the bucket configured by environment/properties.
- New uploads must use the organization/module/entity path strategy.

Examples:

```text
s3_key:
{organizationId}/properties/{propertyId}/uuid_Image1.jpg

filepath:
tamias-dev-files/{organizationId}/properties/{propertyId}
```

```text
s3_key:
{organizationId}/documents/{propertyId}/uuid_Document1.pdf

filepath:
tamias-dev-files/{organizationId}/documents/{propertyId}
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

- Deleting an image deletes the S3 object.
- Deleting an image physically deletes the database row.
- Schema cleanup should remove `deleted_at` from image relationship tables where present.
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
