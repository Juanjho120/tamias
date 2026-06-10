# TAMIAS — Diseño de APIs REST MVP

Este documento define las APIs REST necesarias para implementar el MVP de TAMIAS de forma consistente, segura y mantenible.

---

## 1. Principios generales de API

Todas las APIs deben seguir estas reglas:

1. Usar rutas versionadas bajo `/api/v1`.
2. Usar JSON como formato principal.
3. Usar DTOs, nunca exponer entidades JPA directamente.
4. Validar datos de entrada con Bean Validation.
5. Responder errores con formato estándar.
6. Aplicar seguridad en backend, no solo en frontend.
7. Aplicar filtro multi-tenant en todas las consultas operativas.
8. Usar paginación en listados principales.
9. Usar filtros explícitos en query params.
10. Usar Swagger/OpenAPI para documentación.
11. No confiar en `organizationId` enviado desde frontend.
12. Resolver la organización activa desde el usuario autenticado.

---

## 2. Base URL

```text
/api/v1
```

---

## 3. Rutas principales

```text
/auth
/users
/organizations
/properties
/catalogs
/inventory-items
/maintenance-records
/scheduled-maintenance
/reservations
/task-lists
/purchase-lists
/documents
/ai
/health
```

---

## 4. Authentication API

```http
POST /api/v1/auth/login
GET  /api/v1/auth/me
POST /api/v1/auth/logout
```

---

## 5. Properties API

```http
GET    /api/v1/properties
GET    /api/v1/properties/{id}
POST   /api/v1/properties
PUT    /api/v1/properties/{id}
DELETE /api/v1/properties/{id}
```

Property images:

```http
POST   /api/v1/properties/{id}/images
DELETE /api/v1/properties/{id}/images/{imageId}
```

---

## 6. Catalog APIs

Catálogos incluidos:

```text
maintenance-categories
maintenance-types
maintenance-people
platforms
suppliers
cities
brands
task-templates
```

Patrón estándar:

```http
GET    /api/v1/catalogs/{catalog}
GET    /api/v1/catalogs/{catalog}/{id}
POST   /api/v1/catalogs/{catalog}
PUT    /api/v1/catalogs/{catalog}/{id}
DELETE /api/v1/catalogs/{catalog}/{id}
```

---

## 7. Inventory Items API

`Inventory Items` es el catálogo operativo compartido para mantenimiento, compras, reservaciones y futuros reportes de inventario.

```http
GET    /api/v1/inventory-items
GET    /api/v1/inventory-items/{id}
POST   /api/v1/inventory-items
PUT    /api/v1/inventory-items/{id}
DELETE /api/v1/inventory-items/{id}
```

Filtros soportados:

```http
GET /api/v1/inventory-items?status=ACTIVE
GET /api/v1/inventory-items?itemType=SUPPLY
GET /api/v1/inventory-items?availableForMaintenance=true
GET /api/v1/inventory-items?availableForReservations=true
GET /api/v1/inventory-items?availableForPurchases=true
GET /api/v1/inventory-items?search=shampoo
```

Request:

```json
{
  "name": "Shampoo",
  "description": "Guest shampoo",
  "unit": "unit",
  "itemType": "SUPPLY",
  "internalCode": "SUP-SHAMPOO",
  "barcode": null,
  "availableForMaintenance": false,
  "availableForReservations": true,
  "availableForPurchases": true,
  "status": "ACTIVE"
}
```

---

## 8. Maintenance API

Maintenance records:

```http
GET    /api/v1/maintenance-records
GET    /api/v1/maintenance-records/{id}
POST   /api/v1/maintenance-records
PUT    /api/v1/maintenance-records/{id}
DELETE /api/v1/maintenance-records/{id}
```

Maintenance record items:

```http
GET    /api/v1/maintenance-records/{id}/items
POST   /api/v1/maintenance-records/{id}/items
PUT    /api/v1/maintenance-records/{id}/items/{itemId}
DELETE /api/v1/maintenance-records/{id}/items/{itemId}
```

Maintenance images:

```http
POST   /api/v1/maintenance-records/{id}/images
DELETE /api/v1/maintenance-records/{id}/images/{imageId}
```

---

## 9. Scheduled Maintenance API

```http
GET    /api/v1/scheduled-maintenance
GET    /api/v1/scheduled-maintenance/{id}
POST   /api/v1/scheduled-maintenance
PUT    /api/v1/scheduled-maintenance/{id}
PATCH  /api/v1/scheduled-maintenance/{id}/complete
PATCH  /api/v1/scheduled-maintenance/{id}/reschedule
PATCH  /api/v1/scheduled-maintenance/{id}/cancel
GET    /api/v1/scheduled-maintenance/{id}/history
DELETE /api/v1/scheduled-maintenance/{id}
```

---

## 10. Reservations API

Reservations:

```http
GET    /api/v1/reservations
GET    /api/v1/reservations/calendar
GET    /api/v1/reservations/{id}
POST   /api/v1/reservations
PUT    /api/v1/reservations/{id}
PATCH  /api/v1/reservations/{id}/cancel
DELETE /api/v1/reservations/{id}
```

Reservation supplies:

```http
GET    /api/v1/reservations/{id}/supplies
POST   /api/v1/reservations/{id}/supplies
PUT    /api/v1/reservations/{id}/supplies/{supplyId}
DELETE /api/v1/reservations/{id}/supplies/{supplyId}
```

Supply request:

```json
{
  "inventoryItemId": "uuid",
  "quantity": 2,
  "unit": "unit",
  "notes": "Delivered at check-in"
}
```

---

## 11. Task Lists API

```http
GET    /api/v1/task-lists
GET    /api/v1/task-lists/{id}
POST   /api/v1/task-lists
PUT    /api/v1/task-lists/{id}
DELETE /api/v1/task-lists/{id}
```

Task items:

```http
POST   /api/v1/task-lists/{id}/items
PUT    /api/v1/task-lists/{id}/items/{itemId}
PATCH  /api/v1/task-lists/{id}/items/{itemId}/complete
DELETE /api/v1/task-lists/{id}/items/{itemId}
```

---

## 12. Purchase Lists API

```http
GET    /api/v1/purchase-lists
GET    /api/v1/purchase-lists/{id}
POST   /api/v1/purchase-lists
PUT    /api/v1/purchase-lists/{id}
DELETE /api/v1/purchase-lists/{id}
```

Purchase items:

```http
POST   /api/v1/purchase-lists/{id}/items
PUT    /api/v1/purchase-lists/{id}/items/{itemId}
PATCH  /api/v1/purchase-lists/{id}/items/{itemId}/purchased
DELETE /api/v1/purchase-lists/{id}/items/{itemId}
```

Purchase items reference `inventoryItemId`.

---

## 13. Documents API

```http
GET    /api/v1/documents
GET    /api/v1/documents/{id}
POST   /api/v1/documents
POST   /api/v1/documents/{id}/process
GET    /api/v1/documents/{id}/download-url
DELETE /api/v1/documents/{id}
```

---

## 14. AI API

```http
POST /api/v1/ai/chat
GET  /api/v1/ai/chat-sessions
GET  /api/v1/ai/chat-sessions/{id}
POST /api/v1/ai/documents/search
```

MVP rules:

- RAG answers must be grounded in documents.
- No free SQL execution.
- Future tool calling must expose controlled domain tools.
