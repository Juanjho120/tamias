# TAMIAS — Security Review 6B-2: Properties, Catalogs and Inventory Items

## Scope

This review covers:

```text
Properties
Base Catalogs
Cities
Inventory Items
```

---

## Findings

### 1. Properties already enforce organization isolation

Property reads, updates and deletes already resolve the current organization from the authenticated user and use repository methods scoped by organization.

Current pattern:

```text
currentUserService.getCurrentOrganizationId()
findByIdAndOrganization_IdAndDeletedAtIsNull(...)
```

This is correct for multi-tenant isolation.

### 2. Properties allowed DELETED status through create/update

`PropertyRequest` includes `PropertyStatus`, and `PropertyStatus` includes `DELETED`.

Risk:

```text
POST /properties or PUT /properties/{id} could set status = DELETED
without applying the delete flow that sets deletedAt/deletedBy.
```

Fix:

```text
PropertyService.create(...)
PropertyService.update(...)
```

now reject:

```text
PropertyStatus.DELETED
```

Deletion must go through:

```http
DELETE /api/v1/properties/{id}
```

---

### 3. Base catalogs enforce organization isolation

Base catalog operations already use organization-scoped repository methods:

```text
findByOrganization_IdAndDeletedAtIsNull
findByOrganization_IdAndStatusAndDeletedAtIsNull
findByIdAndOrganization_IdAndDeletedAtIsNull
existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull
```

This is correct for multi-tenant isolation.

---

### 4. Catalogs allowed DELETED status through create/update

`CatalogRequest` includes `CatalogStatus`, and `CatalogStatus` includes `DELETED`.

Risk:

```text
POST /catalogs/{catalog}
PUT /catalogs/{catalog}/{id}
```

could set:

```text
status = DELETED
```

without applying the delete flow.

Fix:

`BaseCatalogService` and `CityService` now reject `CatalogStatus.DELETED` during create/update.

Deletion must go through:

```http
DELETE /api/v1/catalogs/{catalog}/{id}
```

---

### 5. Catalog read permissions were too restrictive

Several catalog services had `@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")` at class level.

Risk:

```text
MAINTENANCE_STAFF and READ_ONLY could be blocked from reading catalogs,
even though read access is needed for operational screens.
```

Fix:

Authorization was moved to method level:

```text
findAll/findById -> ADMINISTRATOR, PROPERTY_MANAGER, MAINTENANCE_STAFF, READ_ONLY
create/update/delete -> ADMINISTRATOR, PROPERTY_MANAGER
```

---

### 6. Inventory Items enforce organization isolation

Inventory item queries are already organization-scoped and search avoids the previous PostgreSQL `lower(bytea)` issue by separating text and non-text search paths.

Fix added:

```text
CatalogStatus.DELETED is now rejected in create/update.
Class-level authorization was replaced by method-level authorization.
```

---

## Files changed

```text
backend/src/main/java/com/tamias/property/service/PropertyService.java
backend/src/main/java/com/tamias/catalog/service/BaseCatalogService.java
backend/src/main/java/com/tamias/catalog/city/service/CityService.java
backend/src/main/java/com/tamias/catalog/inventoryitem/service/InventoryItemService.java
backend/src/main/java/com/tamias/catalog/brand/service/BrandService.java
backend/src/main/java/com/tamias/catalog/maintenancecategory/service/MaintenanceCategoryService.java
backend/src/main/java/com/tamias/catalog/maintenanceperson/service/MaintenancePersonService.java
backend/src/main/java/com/tamias/catalog/maintenancetype/service/MaintenanceTypeService.java
backend/src/main/java/com/tamias/catalog/platform/service/PlatformService.java
backend/src/main/java/com/tamias/catalog/supplier/service/SupplierService.java
backend/src/main/java/com/tamias/catalog/tasktemplate/service/TaskTemplateService.java
docs/11-security-review-properties-catalogs-inventory.md
```

---

## Validation

Backend build:

```bash
cd backend
./mvnw.cmd clean package -DskipTests
```

Manual API checks:

```text
1. Login as Read Only.
2. GET /api/v1/properties -> allowed.
3. GET /api/v1/catalogs/brands -> allowed.
4. GET /api/v1/inventory-items -> allowed.
5. POST /api/v1/properties -> forbidden.
6. POST /api/v1/catalogs/brands -> forbidden.
7. POST /api/v1/inventory-items -> forbidden.
8. Login as Property Manager.
9. POST /api/v1/properties with status DELETED -> 400.
10. PUT /api/v1/catalogs/brands/{id} with status DELETED -> 400.
11. PUT /api/v1/inventory-items/{id} with status DELETED -> 400.
12. DELETE endpoints still perform soft delete correctly.
```
