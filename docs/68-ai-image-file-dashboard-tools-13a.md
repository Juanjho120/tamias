# 13A — AI image/file dashboard tools

Status: Completed.

## Purpose

Add read-only AI tools that let TAMI answer high-level questions about images and files across TAMIAS modules.

This phase builds on:

- 9M — File, image and dashboard analytics tools
- 11A — S3 key strategy + filepath fields
- 11B — Hard delete policy for entity images
- 11C — Hard delete policy for RAG documents
- 12B — Inventory item images
- 12C — Purchase list images
- 12D — Reservation images
- 12E — AI image and inventory brand tools


## Implementation summary

Implemented in backend AI tools only. No frontend changes, no write actions and no database migrations are part of this phase.

Implemented tools:

```text
files.getImageDashboardSummary
files.getRecentUploads
files.getLargestFiles
files.getEntitiesWithoutImages
files.getEntitiesWithMostImages
```

Implementation classes:

```text
backend/src/main/java/com/tamias/ai/tool/handler/FileImageDashboardToolHandler.java
backend/src/main/java/com/tamias/ai/tool/service/AiReadOnlyToolService.java
backend/src/main/java/com/tamias/ai/tool/service/FileImageReadOnlyToolService.java
backend/src/main/java/com/tamias/ai/tool/repository/FileImageToolRepository.java
backend/src/main/java/com/tamias/ai/tool/support/AiToolRoutingSupport.java
```

13A extends the existing 9M file/image/dashboard module instead of creating a duplicate package, because the existing handler already owns file/image/dashboard routing. Module-specific image questions stay owned by `EntityImageToolHandler`.

Additional 13A update:

- Existing `file.storageSummary` and `file.searchMetadata` now include the image tables added in 12B, 12C and 12D.

## User-facing questions

TAMI should be able to answer questions like:

```text
¿Cuántas imágenes tengo en TAMIAS?
¿Cuántas imágenes tengo por módulo?
¿Qué módulo tiene más imágenes?
¿Qué entidades tienen más imágenes?
¿Qué entidades no tienen imágenes?
¿Qué imágenes se subieron recientemente?
¿Qué archivos ocupan más espacio?
¿Cuánto storage tengo registrado por módulo?
¿Qué documentos o imágenes fueron subidos recientemente?
¿Qué propiedades tienen más archivos asociados?
```

## Scope

### Included

- Read-only dashboard-style AI tools for image/file metadata.
- Aggregations by module/entity type.
- Recent uploads across supported modules.
- Largest files by `size_bytes`.
- Counts for entities with and without images where supported.
- Organization-level filtering through current authenticated user context.
- Tool evidence in TAMI responses.

### Not included

- No write operations from IA.
- No S3 cleanup/audit repair yet.
- No deleting orphan files.
- No generating presigned download URLs unless a future requirement explicitly needs it.
- No frontend dashboard changes unless a later phase asks for it.
- No new image tables.

## Proposed tool names

Recommended tools:

```text
files.getImageDashboardSummary
files.getRecentUploads
files.getLargestFiles
files.getEntitiesWithoutImages
files.getEntitiesWithMostImages
```

If existing 9M tools already cover part of this, extend them instead of duplicating behavior.

## Suggested package architecture

Respect the current backend AI package structure:

```text
com.tamias.ai.tool.handler
com.tamias.ai.tool.service
com.tamias.ai.tool.repository
com.tamias.ai.tool.support
```

Preferred approach:

- Reuse or extend the existing file/image/dashboard tool classes from 9M if they are already module-focused and small enough.
- Add a new focused repository/service/handler only if the existing 9M classes would become too large.
- Do not mix these tools into unrelated inventory or purchase handlers.

Potential classes if new files are needed:

```text
FileDashboardToolHandler
FileDashboardReadOnlyToolService
FileDashboardToolRepository
```

## Data sources

Entity image tables:

```text
property_images
maintenance_record_images
inventory_item_images
purchase_images
reservation_images
```

Documents:

```text
documents
document_chunks
```

Optional related parent tables for labels/context:

```text
properties
maintenance_records
inventory_items
purchase_lists
reservations
brands
suppliers
cities
platforms
guests
```

## Multi-tenant rules

Every query must filter by:

```text
organization_id = currentUserService.getCurrentOrganizationId()
```

Do not rely on frontend-provided organization IDs.

Do not return files/images from another organization.

## Image status and hard delete rules

For image tables:

- Count only active image records where applicable.
- Deleted images should not exist physically because image tables use hard delete.
- Do not reference `deleted_at` on image tables that no longer have it.

For documents:

- Documents now use hard delete after 11C/11C.1.
- Do not reference `documents.deleted_at`.
- Exclude failed or unavailable documents only when the specific question requires ready/indexed documents.

## Expected answer style

### Image summary

```text
Resumen de imágenes en TAMIAS:
- Propiedades: 4 imágenes
- Mantenimientos: 3 imágenes
- Items de inventario: 2 imágenes
- Listas de compra: 1 imagen
- Reservaciones: 1 imagen
```

### Entities with most images

```text
Estas entidades tienen más imágenes registradas:
- Propiedad: Bungalow Tu Refugio Perfecto | imágenes: 4
- Item: Limpiador Multisuperficies | marca: Pledge | imágenes: 2
- Reservación: JHK59745 | propiedad: Bungalow Tu Refugio Perfecto | imágenes: 1
```

### Entities without images

```text
Estas entidades no tienen imágenes registradas:

Reservaciones
- HM123456 | propiedad: Tranquilidad y Accesibilidad: Tu Refugio Perfecto | check-in: 2026-07-01

Items de inventario
- Jabón de Baño | marca: — | tipo: SUPPLY
```

### Largest files

```text
Estos son los archivos más grandes registrados:
- receipt.jpg | módulo: compras | tamaño: 3.2 MB
- checkin.jpg | módulo: reservaciones | tamaño: 2.1 MB
```

## Routing notes

The handler should detect terms such as:

```text
imagenes
imágenes
fotos
archivos
files
uploads
subidos
recientes
storage
espacio
pesados
mas grandes
más grandes
sin imagenes
sin imágenes
sin fotos
```

The route should not override specific module tools when the user asks about one module only. Examples:

```text
¿Qué reservaciones tienen imágenes? -> images.getReservationImages
¿Qué items no tienen fotos?         -> images.getInventoryItemImages
¿Qué compras tienen imágenes?       -> images.getPurchaseListImages
```

13A should handle broader cross-module questions:

```text
¿Cuántas imágenes tengo por módulo?
¿Qué entidades no tienen imágenes?
¿Qué archivos ocupan más espacio?
```

## Acceptance tests

1. Ask: `¿Cuántas imágenes tengo por módulo?`
2. Confirm TAMI returns counts across supported modules.
3. Confirm answer includes tool evidence.
4. Ask: `¿Qué entidades tienen más imágenes?`
5. Confirm parent entity labels are readable.
6. Ask: `¿Qué entidades no tienen imágenes?`
7. Confirm results are grouped by module.
8. Ask: `¿Qué imágenes se subieron recientemente?`
9. Confirm recent ordering is based on `created_at`.
10. Ask: `¿Qué archivos ocupan más espacio?`
11. Confirm ordering is based on `size_bytes`.
12. Confirm all queries are restricted to current organization.
13. Confirm no stale `deleted_at` predicates are used against image tables/documents.

## After 13A

13A is complete. Resume the existing AI orchestration roadmap:

```text
9P-G — AI orchestration observability and debug traces
9P-H — Smoke test hardening / final fixes
9P-I — RAG retrieval tuning, only if needed
```
