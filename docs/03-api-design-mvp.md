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
13. Separar administración de usuarios de self-service profile.

---

## 2. Base URL

```text
/api/v1
```

---

## 3. Rutas principales

```text
/auth
/profile
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

Login response includes the current user and the temporary password flag:

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": {
    "id": "uuid",
    "firstName": "Juan",
    "lastName": "Tzun",
    "email": "juan@example.com",
    "role": "ADMINISTRATOR",
    "organization": {
      "id": "uuid",
      "name": "My Organization"
    },
    "passwordChangeRequired": false
  }
}
```

---

## 5. Profile API

The Profile API is available to every authenticated user.

It does not require `ADMINISTRATOR`.

```http
GET   /api/v1/profile
PATCH /api/v1/profile
PATCH /api/v1/profile/password
```

### Get current profile

```http
GET /api/v1/profile
```

Response:

```json
{
  "id": "uuid",
  "firstName": "Juan",
  "lastName": "Tzun",
  "email": "juan@example.com",
  "role": "PROPERTY_MANAGER",
  "organization": {
    "id": "uuid",
    "name": "My Organization"
  },
  "passwordChangeRequired": false
}
```

### Update profile

```http
PATCH /api/v1/profile
```

Request:

```json
{
  "firstName": "Juan",
  "lastName": "Tzun"
}
```

Allowed fields:

```text
firstName
lastName
```

Email and role are not changed from this endpoint.

### Change password

```http
PATCH /api/v1/profile/password
```

Request:

```json
{
  "currentPassword": "temporary-or-current-password",
  "newPassword": "new-secure-password",
  "confirmNewPassword": "new-secure-password"
}
```

Rules:

- Current password is required.
- New password must be different from current password.
- Confirmation must match.
- After success, `passwordChangeRequired` becomes `false`.

---

## 6. Users API

The Users API is intended for administrators.

```http
GET    /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

Administrator responsibilities:

- Create users.
- Assign roles.
- Update status.
- Activate/deactivate users.
- Delete users.

When an administrator creates a user, the initial password is temporary and the backend must set:

```text
password_change_required = true
```

Create request:

```json
{
  "firstName": "Ana",
  "lastName": "Lopez",
  "email": "ana@example.com",
  "password": "temporary-password",
  "role": "PROPERTY_MANAGER"
}
```

Update request:

```json
{
  "firstName": "Ana",
  "lastName": "Lopez",
  "email": "ana@example.com",
  "role": "PROPERTY_MANAGER",
  "status": "ACTIVE"
}
```

---

## 7. Properties API

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

## 8. Catalog APIs

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

## 9. Inventory Items API

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

---

## 10. Maintenance API

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

## 11. Reservations API

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

---

## 12. Documents API

```http
GET    /api/v1/documents
GET    /api/v1/documents/{id}
POST   /api/v1/documents
POST   /api/v1/documents/{id}/process
GET    /api/v1/documents/{id}/download-url
DELETE /api/v1/documents/{id}
```

---

## 13. AI API

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
