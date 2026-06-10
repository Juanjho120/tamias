# TAMIAS — Security Review 6B-5: Purchases, Tasks, Documents and AI

## Scope

This review covers:

```text
Purchase Lists
Purchase Items
Task Lists
Task Items
Documents
Document processing/indexing
AI chat sessions
AI document search/RAG
```

---

## Findings and changes

### 1. Purchase Lists already enforce organization isolation

`PurchaseListService` already resolves:

```text
currentUserService.getCurrentOrganizationId()
```

and uses organization-scoped repository methods for:

```text
findByIdAndOrganization_IdAndDeletedAtIsNull
findByOrganization_IdAndDeletedAtIsNull
findByOrganization_IdAndProperty_IdAndDeletedAtIsNull
findByOrganization_IdAndSupplier_IdAndDeletedAtIsNull
findByOrganization_IdAndCity_IdAndDeletedAtIsNull
```

This is correct for multi-tenant isolation.

---

### 2. Purchase Lists allowed DELETED status through create/update

`PurchaseListRequest` includes:

```text
PurchaseListStatus status
```

and `PurchaseListStatus` includes:

```text
DELETED
```

Risk:

```text
POST /api/v1/purchase-lists
PUT /api/v1/purchase-lists/{id}
```

could set:

```text
status = DELETED
```

without applying the delete flow that sets:

```text
deletedAt
deletedBy
updatedBy
```

Fix:

`PurchaseListService.create(...)` and `PurchaseListService.update(...)` now reject `PurchaseListStatus.DELETED`.

Deletion must go through:

```http
DELETE /api/v1/purchase-lists/{id}
```

---

### 3. Purchase item mutations now revalidate the parent purchase list

The service already scoped purchase item access by:

```text
itemId
purchaseListId
organizationId
```

Fix added:

These methods now first revalidate the parent purchase list using:

```text
findPurchaseList(purchaseListId)
```

Methods:

```text
updateItem
updateItemPurchased
deleteItem
```

This prevents mutating items for a deleted parent purchase list.

---

### 4. Purchase items now enforce inventory item availability

When a purchase item references an Inventory Item, the backend now validates:

```text
same organization
not deleted
CatalogStatus.ACTIVE
availableForPurchases = true
```

This prevents using inactive items or items not available for purchase lists.

---

### 5. Task Lists already enforce organization isolation

`TaskListService` already resolves:

```text
currentUserService.getCurrentOrganizationId()
```

and uses organization-scoped repository methods for task lists.

It also validates linked entities through organization-scoped repositories:

```text
property
reservation
maintenanceRecord
taskTemplate
```

This is correct for multi-tenant isolation.

---

### 6. Task Lists allowed DELETED status through create/update

`TaskListRequest` includes:

```text
TaskListStatus status
```

and `TaskListStatus` includes:

```text
DELETED
```

Risk:

```text
POST /api/v1/task-lists
PUT /api/v1/task-lists/{id}
```

could set:

```text
status = DELETED
```

without applying the delete flow that sets:

```text
deletedAt
deletedBy
updatedBy
```

Fix:

`TaskListService.create(...)` and `TaskListService.update(...)` now reject `TaskListStatus.DELETED`.

Deletion must go through:

```http
DELETE /api/v1/task-lists/{id}
```

---

### 7. Task item mutations now revalidate the parent task list

The service already scoped task item access by:

```text
itemId
taskListId
organizationId
```

Fix added:

These methods now first revalidate the parent task list using:

```text
findTaskList(taskListId)
```

Methods:

```text
updateItem
updateItemCompletion
deleteItem
```

This prevents mutating items for a deleted parent task list.

---

### 8. Task templates must be active when used

When a task item references a Task Template, the backend now validates:

```text
same organization
not deleted
CatalogStatus.ACTIVE
```

This prevents creating/updating task items from inactive task templates.

---

## Documents review

The current Document module already uses organization-scoped repository methods such as:

```text
findByIdAndOrganization_IdAndDeletedAtIsNull
findByOrganization_IdAndDeletedAtIsNull
findByOrganization_IdAndProperty_IdAndDeletedAtIsNull
findByOrganization_IdAndDocumentTypeAndDeletedAtIsNull
findByOrganization_IdAndProcessingStatusAndDeletedAtIsNull
findByOrganization_IdAndStatusAndDeletedAtIsNull
```

The controller exposes operations for:

```text
list
find by id
upload
download URL
direct file retrieval
process
index
chunks
delete
```

Important follow-up for a later sub-block:

```text
Review DocumentService implementation line-by-line before changing it.
```

Do not modify document storage or indexing blindly, because it involves:

```text
S3/local storage
document processing
chunking
vector indexing
download URLs
```

---

## AI review

The current AI repositories scope sessions/messages by organization:

```text
AiChatSessionRepository.findByIdAndOrganization_Id
AiChatSessionRepository.findByOrganization_Id
AiChatSessionRepository.findByOrganization_IdAndProperty_Id
AiChatMessageRepository.findByChatSession_IdAndOrganization_IdOrderByCreatedAtAsc
AiChatMessageRepository.countByChatSession_IdAndOrganization_Id
```

Important follow-up for a later sub-block:

```text
Review AI service and RAG search implementation line-by-line before changing it.
```

The most important AI rule remains:

```text
RAG searches and AI answers must only use documents from the current organization.
```

---

## Files changed

```text
backend/src/main/java/com/tamias/purchase/service/PurchaseListService.java
backend/src/main/java/com/tamias/task/service/TaskListService.java
docs/14-security-review-purchases-tasks-documents-ai.md
```

---

## Validation

Backend build:

```bash
cd backend
./mvnw.cmd clean package -DskipTests
```

Manual API smoke tests:

```text
1. Login as Read Only.
2. GET /api/v1/purchase-lists -> allowed.
3. GET /api/v1/task-lists -> allowed.
4. POST /api/v1/purchase-lists -> forbidden.
5. POST /api/v1/task-lists -> forbidden.

6. Login as Maintenance Staff.
7. POST /api/v1/purchase-lists -> allowed.
8. POST /api/v1/purchase-lists with status DELETED -> 400.
9. Add purchase item using inactive inventory item -> 400.
10. Add purchase item using availableForPurchases=false -> 400.

11. Login as Property Manager.
12. POST /api/v1/task-lists with status DELETED -> 400.
13. PUT /api/v1/task-lists/{id} with status DELETED -> 400.
14. Create task item using inactive task template -> 400.
15. DELETE /api/v1/task-lists/{id} -> soft delete.
```
