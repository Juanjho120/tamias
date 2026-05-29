# TAMIAS — Diseño de APIs REST MVP

Este documento define el diseño inicial de APIs REST para el MVP de TAMIAS.

Debe usarse como fuente de verdad para:

- Controllers en Spring Boot.
- Services.
- DTOs de request y response.
- Validaciones.
- Seguridad por endpoint.
- Guards y servicios Angular.
- Integración con Swagger/OpenAPI.
- Pruebas de backend.
- Futuras integraciones con IA mediante tool calling.

Este diseño se basa en los documentos:

- `01-architecture-mvp.md`
- `PROJECT_CONTEXT.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `02-database-design-mvp.md`

---

## 1. Objetivo

El objetivo de este documento es definir las APIs REST necesarias para implementar el MVP de TAMIAS de forma consistente, segura y mantenible.

El MVP incluye:

- Authentication
- Organizations
- Users
- Roles básicos
- Properties
- Catalogs
- Maintenance
- Scheduled Maintenance
- Reservations
- Task Lists
- Purchase Lists
- Documents
- AI Document Search con RAG
- Basic Deploy

---

## 2. Principios generales de API

Todas las APIs deben seguir estas reglas:

1. Usar rutas versionadas bajo `/api/v1`.
2. Usar JSON como formato principal.
3. Usar DTOs, nunca exponer entidades JPA directamente.
4. Validar datos de entrada con Bean Validation.
5. Responder errores con un formato estándar.
6. Aplicar seguridad en backend, no solo en frontend.
7. Aplicar filtro multi-tenant en todas las consultas operativas.
8. Usar paginación en listados principales.
9. Usar filtros explícitos en query params.
10. Usar Swagger/OpenAPI para documentación.
11. No confiar en `organizationId` enviado desde frontend para datos operativos.
12. Resolver la organización activa desde el usuario autenticado.

---

## 3. Convenciones REST

### 3.1 Base URL

```text
/api/v1
```

### 3.2 Naming

Usar sustantivos en plural:

```text
/properties
/maintenance-records
/scheduled-maintenance
/purchase-lists
/documents
```

### 3.3 Métodos HTTP

| Método | Uso |
|---|---|
| GET | Consultar datos |
| POST | Crear recursos o ejecutar acciones |
| PUT | Actualizar recurso completo |
| PATCH | Actualización parcial o cambio de estado |
| DELETE | Eliminación lógica o eliminación controlada |

### 3.4 Códigos HTTP

| Código | Uso |
|---|---|
| 200 OK | Consulta o actualización exitosa |
| 201 Created | Creación exitosa |
| 204 No Content | Eliminación exitosa sin body |
| 400 Bad Request | Error de validación o datos inválidos |
| 401 Unauthorized | No autenticado |
| 403 Forbidden | Sin permisos |
| 404 Not Found | Recurso no existe o no pertenece a la organización |
| 409 Conflict | Conflicto de unicidad o estado |
| 500 Internal Server Error | Error inesperado |

---

## 4. Seguridad

### 4.1 Autenticación

El sistema usará JWT.

Todas las rutas estarán protegidas excepto:

```text
POST /api/v1/auth/login
GET  /api/v1/health
GET  /api/v1/public/*
```

### 4.2 Header de autenticación

```http
Authorization: Bearer {access_token}
```

### 4.3 Roles iniciales

```text
ADMINISTRATOR
PROPERTY_MANAGER
MAINTENANCE_STAFF
READ_ONLY
```

### 4.4 Reglas por rol

| Módulo | Administrator | Property Manager | Maintenance Staff | Read Only |
|---|---:|---:|---:|---:|
| Users | Full | No | No | Read own |
| Properties | Full | CRUD | Read assigned | Read |
| Catalogs | Full | CRUD | Read | Read |
| Maintenance | Full | CRUD | Limited CRUD | Read |
| Scheduled Maintenance | Full | CRUD | Update assigned | Read |
| Reservations | Full | CRUD | Read limited | Read |
| Tasks | Full | CRUD | Update assigned | Read |
| Purchases | Full | CRUD | Read limited | Read |
| Documents | Full | CRUD | Read limited | Read |
| AI Assistant | Full | Use | Use limited | Use read-only |

Nota:

En MVP puede implementarse seguridad por rol de forma simple. Los permisos granulares pueden quedar para una fase posterior.

---

## 5. Multi-tenancy en APIs

TAMIAS usa:

```text
Shared database + shared schema + organization_id
```

Reglas:

1. El backend obtiene la organización activa desde el usuario autenticado.
2. El frontend no debe enviar `organizationId` para operaciones normales.
3. Todos los listados deben filtrar por organización.
4. Todas las búsquedas por ID deben validar que el recurso pertenezca a la organización.
5. Si el recurso no pertenece a la organización, responder `404 Not Found`, no `403`, para evitar filtrar existencia de datos.

Ejemplo correcto:

```text
GET /api/v1/properties/{id}
```

El backend valida internamente:

```text
property.id = {id}
AND property.organization_id = currentUser.organization_id
```

---

## 6. Formato estándar de respuesta

### 6.1 Respuesta simple

```json
{
  "id": "uuid",
  "name": "Casa Vista Hermosa",
  "status": "ACTIVE"
}
```

### 6.2 Respuesta paginada

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 125,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

### 6.3 Error estándar

```json
{
  "timestamp": "2026-05-29T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/properties",
  "details": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ]
}
```

---

## 7. Paginación, filtros y ordenamiento

### 7.1 Query params estándar

```text
page=0
size=20
sort=createdAt,desc
```

Ejemplo:

```http
GET /api/v1/properties?page=0&size=20&sort=name,asc
```

### 7.2 Filtros comunes

```text
status
search
from
to
propertyId
```

Ejemplo:

```http
GET /api/v1/maintenance-records?propertyId={id}&from=2026-01-01&to=2026-12-31
```

### 7.3 Tamaño máximo

Recomendación:

```text
size máximo = 100
size default = 20
```

---

# 8. Authentication API

## 8.1 Login

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "admin@tamias.com",
  "password": "password"
}
```

Validaciones:

- `email` requerido y con formato válido.
- `password` requerido.

Response `200 OK`:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "firstName": "Juan",
    "lastName": "Tzun",
    "email": "admin@tamias.com",
    "role": "ADMINISTRATOR",
    "organization": {
      "id": "uuid",
      "name": "TAMIAS Demo"
    }
  }
}
```

Errores:

- `401 Unauthorized` si credenciales inválidas.
- `403 Forbidden` si usuario está inactivo.

---

## 8.2 Current user

```http
GET /api/v1/auth/me
```

Response:

```json
{
  "id": "uuid",
  "firstName": "Juan",
  "lastName": "Tzun",
  "email": "admin@tamias.com",
  "role": "ADMINISTRATOR",
  "organization": {
    "id": "uuid",
    "name": "TAMIAS Demo"
  }
}
```

Seguridad:

- Requiere autenticación.

---

## 8.3 Logout

```http
POST /api/v1/auth/logout
```

MVP:

- Puede responder `204 No Content`.
- Si no se implementa blacklist de tokens, el logout real lo maneja el frontend eliminando el token local.

Response:

```http
204 No Content
```

---

# 9. Organizations API

En MVP la organización puede ser creada por seed o durante onboarding inicial.

## 9.1 Get current organization

```http
GET /api/v1/organizations/current
```

Response:

```json
{
  "id": "uuid",
  "name": "TAMIAS Demo",
  "description": "Demo organization",
  "status": "ACTIVE",
  "createdAt": "2026-05-29T12:00:00Z",
  "updatedAt": "2026-05-29T12:00:00Z"
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- READ_ONLY

---

## 9.2 Update current organization

```http
PUT /api/v1/organizations/current
```

Request:

```json
{
  "name": "TAMIAS Demo",
  "description": "Organization for demo properties"
}
```

Response:

```json
{
  "id": "uuid",
  "name": "TAMIAS Demo",
  "description": "Organization for demo properties",
  "status": "ACTIVE"
}
```

Seguridad:

- ADMINISTRATOR

---

# 10. Users API

## 10.1 List users

```http
GET /api/v1/users
```

Query params:

```text
page
size
sort
status
search
role
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "firstName": "Juan",
      "lastName": "Tzun",
      "email": "juan@example.com",
      "role": "ADMINISTRATOR",
      "status": "ACTIVE",
      "createdAt": "2026-05-29T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Seguridad:

- ADMINISTRATOR

---

## 10.2 Get user by ID

```http
GET /api/v1/users/{id}
```

Seguridad:

- ADMINISTRATOR
- Usuario autenticado puede consultar su propio usuario.

---

## 10.3 Create user

```http
POST /api/v1/users
```

Request:

```json
{
  "firstName": "Mario",
  "lastName": "Lopez",
  "email": "mario@example.com",
  "password": "TempPassword123!",
  "roleCode": "PROPERTY_MANAGER"
}
```

Validaciones:

- `firstName` requerido.
- `lastName` requerido.
- `email` requerido y único globalmente.
- `password` requerido en MVP.
- `roleCode` requerido y válido.

Response `201 Created`:

```json
{
  "id": "uuid",
  "firstName": "Mario",
  "lastName": "Lopez",
  "email": "mario@example.com",
  "role": "PROPERTY_MANAGER",
  "status": "ACTIVE"
}
```

Seguridad:

- ADMINISTRATOR

Nota:

Invitaciones por correo quedan fuera del MVP inicial. En MVP se permite crear usuario con contraseña temporal.

---

## 10.4 Update user

```http
PUT /api/v1/users/{id}
```

Request:

```json
{
  "firstName": "Mario",
  "lastName": "Lopez",
  "roleCode": "PROPERTY_MANAGER",
  "status": "ACTIVE"
}
```

Seguridad:

- ADMINISTRATOR

---

## 10.5 Delete user

```http
DELETE /api/v1/users/{id}
```

Comportamiento:

- Soft delete.
- Cambiar status a `DELETED`.

Response:

```http
204 No Content
```

Seguridad:

- ADMINISTRATOR

---

# 11. Properties API

## 11.1 List properties

```http
GET /api/v1/properties
```

Query params:

```text
page
size
sort
status
search
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Casa Vista Hermosa",
      "address": "Sololá, Guatemala",
      "description": "Casa vacacional",
      "status": "ACTIVE",
      "coverImageUrl": "https://...",
      "createdAt": "2026-05-29T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF
- READ_ONLY

---

## 11.2 Get property by ID

```http
GET /api/v1/properties/{id}
```

Response:

```json
{
  "id": "uuid",
  "name": "Casa Vista Hermosa",
  "address": "Sololá, Guatemala",
  "description": "Casa vacacional",
  "status": "ACTIVE",
  "images": [
    {
      "id": "uuid",
      "url": "https://...",
      "isCover": true
    }
  ],
  "createdAt": "2026-05-29T12:00:00Z",
  "updatedAt": "2026-05-29T12:00:00Z"
}
```

---

## 11.3 Create property

```http
POST /api/v1/properties
```

Request:

```json
{
  "name": "Casa Vista Hermosa",
  "address": "Sololá, Guatemala",
  "description": "Casa vacacional",
  "status": "ACTIVE"
}
```

Validaciones:

- `name` requerido.
- `name` único por organización.
- `status` válido.

Response:

```http
201 Created
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 11.4 Update property

```http
PUT /api/v1/properties/{id}
```

Request:

```json
{
  "name": "Casa Vista Hermosa",
  "address": "Sololá, Guatemala",
  "description": "Casa vacacional actualizada",
  "status": "ACTIVE"
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 11.5 Delete property

```http
DELETE /api/v1/properties/{id}
```

Comportamiento:

- Soft delete.
- Cambiar status a `DELETED`.

Seguridad:

- ADMINISTRATOR

---

## 11.6 Upload property image

```http
POST /api/v1/properties/{id}/images
Content-Type: multipart/form-data
```

Request:

```text
file: image file
isCover: true/false
```

Validaciones:

- Archivo requerido.
- Tipo permitido: JPG, PNG, WEBP.
- Tamaño máximo definido por configuración.
- Propiedad debe pertenecer a la organización.

Response:

```json
{
  "id": "uuid",
  "originalFilename": "front.jpg",
  "url": "https://...",
  "contentType": "image/jpeg",
  "sizeBytes": 245000,
  "isCover": true
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

# 12. Catalog APIs

Para evitar duplicar demasiado código, se puede implementar un controller por catálogo o un patrón reutilizable por tipo.

Para MVP se recomienda controllers separados para claridad.

Catálogos incluidos:

```text
maintenance-categories
maintenance-types
maintenance-people
platforms
suppliers
cities
materials
brands
task-templates
```

---

## 12.1 Patrón estándar de catálogo

Ejemplo con maintenance categories.

### List

```http
GET /api/v1/maintenance-categories
```

Query params:

```text
page
size
sort
status
search
```

### Get by ID

```http
GET /api/v1/maintenance-categories/{id}
```

### Create

```http
POST /api/v1/maintenance-categories
```

Request:

```json
{
  "name": "Water Filters",
  "description": "Water filter maintenance category",
  "status": "ACTIVE"
}
```

### Update

```http
PUT /api/v1/maintenance-categories/{id}
```

Request:

```json
{
  "name": "Water Filters",
  "description": "Updated description",
  "status": "ACTIVE"
}
```

### Delete

```http
DELETE /api/v1/maintenance-categories/{id}
```

Comportamiento:

- Soft delete.
- Status `DELETED`.

Seguridad:

- List/Get: todos los roles autenticados.
- Create/Update: ADMINISTRATOR, PROPERTY_MANAGER.
- Delete: ADMINISTRATOR.

---

## 12.2 Endpoints de catálogos

Aplicar el patrón anterior a:

```http
/api/v1/maintenance-categories
/api/v1/maintenance-types
/api/v1/maintenance-people
/api/v1/platforms
/api/v1/suppliers
/api/v1/cities
/api/v1/materials
/api/v1/brands
/api/v1/task-templates
```

### DTO estándar simple

Request:

```json
{
  "name": "Name",
  "description": "Optional description",
  "status": "ACTIVE"
}
```

Response:

```json
{
  "id": "uuid",
  "name": "Name",
  "description": "Optional description",
  "status": "ACTIVE",
  "createdAt": "2026-05-29T12:00:00Z",
  "updatedAt": "2026-05-29T12:00:00Z"
}
```

### DTOs especializados

Algunos catálogos requieren campos extra.

#### Maintenance people

```json
{
  "fullName": "Pedro López",
  "phone": "5555-5555",
  "email": "pedro@example.com",
  "notes": "Electrician",
  "status": "ACTIVE"
}
```

#### Suppliers

```json
{
  "name": "EPA",
  "phone": "5555-5555",
  "email": "contacto@epa.com",
  "website": "https://example.com",
  "notes": "Hardware supplier",
  "status": "ACTIVE"
}
```

#### Cities

```json
{
  "name": "Guatemala",
  "country": "Guatemala",
  "status": "ACTIVE"
}
```

#### Materials

```json
{
  "name": "Water filter",
  "description": "Sediment filter",
  "unit": "unit",
  "status": "ACTIVE"
}
```

---

# 13. Maintenance Records API

## 13.1 List maintenance records

```http
GET /api/v1/maintenance-records
```

Query params:

```text
page
size
sort
propertyId
categoryId
typeId
from
to
status
search
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "property": {
        "id": "uuid",
        "name": "Casa Vista Hermosa"
      },
      "category": {
        "id": "uuid",
        "name": "Water Filters"
      },
      "type": {
        "id": "uuid",
        "name": "Replacement"
      },
      "maintenanceDate": "2026-05-29T09:00:00Z",
      "description": "Changed sediment filters",
      "cost": 150.00,
      "status": "COMPLETED"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF
- READ_ONLY

---

## 13.2 Get maintenance record by ID

```http
GET /api/v1/maintenance-records/{id}
```

Response incluye:

- Datos principales.
- Responsables.
- Materiales usados.
- Imágenes.

```json
{
  "id": "uuid",
  "property": {
    "id": "uuid",
    "name": "Casa Vista Hermosa"
  },
  "category": {
    "id": "uuid",
    "name": "Water Filters"
  },
  "type": {
    "id": "uuid",
    "name": "Replacement"
  },
  "maintenanceDate": "2026-05-29T09:00:00Z",
  "description": "Changed sediment filters",
  "cost": 150.00,
  "responsiblePeople": [
    {
      "id": "uuid",
      "fullName": "Pedro López"
    }
  ],
  "materialsUsed": [
    {
      "id": "uuid",
      "materialId": "uuid",
      "materialName": "Sediment filter",
      "quantity": 2,
      "unit": "unit"
    }
  ],
  "images": [
    {
      "id": "uuid",
      "url": "https://..."
    }
  ],
  "status": "COMPLETED"
}
```

---

## 13.3 Create maintenance record

```http
POST /api/v1/maintenance-records
```

Request:

```json
{
  "propertyId": "uuid",
  "categoryId": "uuid",
  "typeId": "uuid",
  "maintenanceDate": "2026-05-29T09:00:00Z",
  "description": "Changed sediment filters",
  "cost": 150.00,
  "responsiblePersonIds": [
    "uuid"
  ],
  "materialsUsed": [
    {
      "materialId": "uuid",
      "materialName": "Sediment filter",
      "quantity": 2,
      "unit": "unit",
      "notes": "Changed first stage filters"
    }
  ]
}
```

Validaciones:

- `propertyId` requerido y debe pertenecer a la organización.
- `categoryId` requerido y debe pertenecer a la organización.
- `typeId` requerido y debe pertenecer a la organización.
- `maintenanceDate` requerido.
- `description` requerido.
- `cost` no debe ser negativo.
- Responsables deben pertenecer a la organización.
- Materiales deben pertenecer a la organización.

Response:

```http
201 Created
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF

---

## 13.4 Update maintenance record

```http
PUT /api/v1/maintenance-records/{id}
```

Request similar a create.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF con restricciones.

---

## 13.5 Delete maintenance record

```http
DELETE /api/v1/maintenance-records/{id}
```

Comportamiento:

- Soft delete.
- Status `DELETED`.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 13.6 Upload maintenance image

```http
POST /api/v1/maintenance-records/{id}/images
Content-Type: multipart/form-data
```

Request:

```text
file: image file
```

Response:

```json
{
  "id": "uuid",
  "originalFilename": "filter.jpg",
  "url": "https://...",
  "contentType": "image/jpeg",
  "sizeBytes": 123456
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF

---

# 14. Scheduled Maintenance API

## 14.1 List scheduled maintenance

```http
GET /api/v1/scheduled-maintenance
```

Query params:

```text
page
size
sort
propertyId
categoryId
typeId
from
to
status
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "property": {
        "id": "uuid",
        "name": "Casa Vista Hermosa"
      },
      "category": {
        "id": "uuid",
        "name": "Water Filters"
      },
      "type": {
        "id": "uuid",
        "name": "Inspection"
      },
      "plannedDate": "2026-06-15",
      "plannedTime": "09:00",
      "description": "Inspect filters",
      "status": "SCHEDULED"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

## 14.2 Calendar view

```http
GET /api/v1/scheduled-maintenance/calendar
```

Query params:

```text
from
to
propertyId
```

Response:

```json
[
  {
    "id": "uuid",
    "title": "Water Filters - Inspection",
    "start": "2026-06-15T09:00:00",
    "end": null,
    "status": "SCHEDULED",
    "propertyName": "Casa Vista Hermosa"
  }
]
```

Uso:

- FullCalendar en Angular.

---

## 14.3 Get scheduled maintenance by ID

```http
GET /api/v1/scheduled-maintenance/{id}
```

Incluye historial.

Response:

```json
{
  "id": "uuid",
  "property": {
    "id": "uuid",
    "name": "Casa Vista Hermosa"
  },
  "category": {
    "id": "uuid",
    "name": "Water Filters"
  },
  "type": {
    "id": "uuid",
    "name": "Inspection"
  },
  "plannedDate": "2026-06-15",
  "plannedTime": "09:00",
  "description": "Inspect filters",
  "status": "SCHEDULED",
  "history": [
    {
      "id": "uuid",
      "previousStatus": "SCHEDULED",
      "newStatus": "RESCHEDULED",
      "reason": "Supplier unavailable",
      "changedAt": "2026-06-10T14:00:00Z"
    }
  ]
}
```

---

## 14.4 Create scheduled maintenance

```http
POST /api/v1/scheduled-maintenance
```

Request:

```json
{
  "propertyId": "uuid",
  "categoryId": "uuid",
  "typeId": "uuid",
  "plannedDate": "2026-06-15",
  "plannedTime": "09:00",
  "description": "Inspect filters"
}
```

Validaciones:

- `propertyId` requerido.
- `categoryId` requerido.
- `typeId` requerido.
- `plannedDate` requerido.
- Entidades relacionadas deben pertenecer a la organización.

Response:

```http
201 Created
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 14.5 Update scheduled maintenance

```http
PUT /api/v1/scheduled-maintenance/{id}
```

Request:

```json
{
  "propertyId": "uuid",
  "categoryId": "uuid",
  "typeId": "uuid",
  "plannedDate": "2026-06-15",
  "plannedTime": "09:00",
  "description": "Inspect filters"
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 14.6 Reschedule scheduled maintenance

```http
PATCH /api/v1/scheduled-maintenance/{id}/reschedule
```

Request:

```json
{
  "newPlannedDate": "2026-06-20",
  "newPlannedTime": "10:00",
  "reason": "Supplier unavailable"
}
```

Validaciones:

- `newPlannedDate` requerido.
- `reason` requerido.

Comportamiento:

- Cambiar estado a `RESCHEDULED`.
- Actualizar fecha y hora.
- Crear registro en `scheduled_maintenance_history`.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 14.7 Cancel scheduled maintenance

```http
PATCH /api/v1/scheduled-maintenance/{id}/cancel
```

Request:

```json
{
  "reason": "No longer needed"
}
```

Validaciones:

- `reason` requerido.

Comportamiento:

- Cambiar estado a `CANCELLED`.
- Guardar razón.
- Crear historial.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 14.8 Complete scheduled maintenance

```http
PATCH /api/v1/scheduled-maintenance/{id}/complete
```

Request:

```json
{
  "maintenanceRecordId": "uuid"
}
```

Comportamiento:

- Cambiar estado a `COMPLETED`.
- Relacionar con maintenance record.
- Crear historial.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF

---

# 15. Reservations API

## 15.1 List reservations

```http
GET /api/v1/reservations
```

Query params:

```text
page
size
sort
propertyId
platformId
from
to
status
search
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "property": {
        "id": "uuid",
        "name": "Casa Vista Hermosa"
      },
      "platform": {
        "id": "uuid",
        "name": "Airbnb"
      },
      "reservationCode": "HM12345",
      "checkIn": "2026-07-01",
      "checkOut": "2026-07-05",
      "guestNames": [
        "John Smith"
      ],
      "reservationValue": 450.00,
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

## 15.2 Get reservation by ID

```http
GET /api/v1/reservations/{id}
```

Response:

```json
{
  "id": "uuid",
  "property": {
    "id": "uuid",
    "name": "Casa Vista Hermosa"
  },
  "platform": {
    "id": "uuid",
    "name": "Airbnb"
  },
  "reservationCode": "HM12345",
  "checkIn": "2026-07-01",
  "checkOut": "2026-07-05",
  "suppliesDelivered": true,
  "observations": "Late check-in",
  "reservationValue": 450.00,
  "invoiceNumber": "123",
  "invoiceSeries": "A",
  "guests": [
    {
      "id": "uuid",
      "fullName": "John Smith",
      "email": "john@example.com",
      "phone": "5555-5555",
      "primary": true
    }
  ],
  "status": "ACTIVE"
}
```

---

## 15.3 Create reservation

```http
POST /api/v1/reservations
```

Request:

```json
{
  "propertyId": "uuid",
  "platformId": "uuid",
  "reservationCode": "HM12345",
  "checkIn": "2026-07-01",
  "checkOut": "2026-07-05",
  "suppliesDelivered": true,
  "observations": "Late check-in",
  "reservationValue": 450.00,
  "invoiceNumber": "123",
  "invoiceSeries": "A",
  "guests": [
    {
      "guestId": "uuid",
      "fullName": "John Smith",
      "email": "john@example.com",
      "phone": "5555-5555",
      "primary": true
    }
  ]
}
```

Validaciones:

- `propertyId` requerido.
- `checkIn` requerido.
- `checkOut` requerido.
- `checkOut` debe ser mayor que `checkIn`.
- `reservationValue` no debe ser negativo.
- Plataforma debe pertenecer a la organización si se envía.
- Guest puede ser existente por `guestId` o nuevo por datos.

Response:

```http
201 Created
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 15.4 Update reservation

```http
PUT /api/v1/reservations/{id}
```

Request similar a create.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 15.5 Cancel reservation

```http
PATCH /api/v1/reservations/{id}/cancel
```

Request:

```json
{
  "reason": "Guest cancelled"
}
```

MVP:

- Puede cambiar status a `CANCELLED`.
- La razón puede agregarse a observations si no hay campo específico todavía.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 15.6 Delete reservation

```http
DELETE /api/v1/reservations/{id}
```

Comportamiento:

- Soft delete.
- Status `DELETED`.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

# 16. Guests API

## 16.1 List guests

```http
GET /api/v1/guests
```

Query params:

```text
page
size
sort
status
search
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- READ_ONLY

---

## 16.2 Create guest

```http
POST /api/v1/guests
```

Request:

```json
{
  "fullName": "John Smith",
  "email": "john@example.com",
  "phone": "5555-5555",
  "notes": "Frequent guest",
  "status": "ACTIVE"
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 16.3 Update guest

```http
PUT /api/v1/guests/{id}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 16.4 Delete guest

```http
DELETE /api/v1/guests/{id}
```

Comportamiento:

- Soft delete.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

# 17. Task Lists API

## 17.1 List task lists

```http
GET /api/v1/task-lists
```

Query params:

```text
page
size
sort
propertyId
reservationId
maintenanceRecordId
status
dueFrom
dueTo
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "property": {
        "id": "uuid",
        "name": "Casa Vista Hermosa"
      },
      "title": "Pre check-in tasks",
      "creationDate": "2026-07-01",
      "dueDate": "2026-07-01",
      "status": "OPEN",
      "completedItems": 2,
      "totalItems": 5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

## 17.2 Get task list by ID

```http
GET /api/v1/task-lists/{id}
```

Response:

```json
{
  "id": "uuid",
  "property": {
    "id": "uuid",
    "name": "Casa Vista Hermosa"
  },
  "reservationId": "uuid",
  "maintenanceRecordId": null,
  "title": "Pre check-in tasks",
  "creationDate": "2026-07-01",
  "dueDate": "2026-07-01",
  "status": "OPEN",
  "items": [
    {
      "id": "uuid",
      "taskTemplateId": "uuid",
      "taskName": "Check towels",
      "responsiblePerson": "Pedro",
      "completed": false,
      "completionDate": null,
      "sortOrder": 1
    }
  ]
}
```

---

## 17.3 Create task list

```http
POST /api/v1/task-lists
```

Request:

```json
{
  "propertyId": "uuid",
  "reservationId": "uuid",
  "maintenanceRecordId": null,
  "title": "Pre check-in tasks",
  "creationDate": "2026-07-01",
  "dueDate": "2026-07-01",
  "items": [
    {
      "taskTemplateId": "uuid",
      "taskName": "Check towels",
      "responsiblePerson": "Pedro",
      "sortOrder": 1
    }
  ]
}
```

Validaciones:

- `propertyId` requerido.
- `title` requerido.
- Si `reservationId` se envía, debe pertenecer a la organización.
- Si `maintenanceRecordId` se envía, debe pertenecer a la organización.
- Items no deben estar vacíos.

Response:

```http
201 Created
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF

---

## 17.4 Update task list

```http
PUT /api/v1/task-lists/{id}
```

Request similar a create.

---

## 17.5 Complete task item

```http
PATCH /api/v1/task-lists/{taskListId}/items/{itemId}/complete
```

Request:

```json
{
  "completed": true
}
```

Comportamiento:

- Si `completed = true`, setear `completionDate`.
- Si `completed = false`, limpiar `completionDate`.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF

---

## 17.6 Delete task list

```http
DELETE /api/v1/task-lists/{id}
```

Comportamiento:

- Soft delete.
- Status `DELETED`.

---

# 18. Purchase Lists API

## 18.1 List purchase lists

```http
GET /api/v1/purchase-lists
```

Query params:

```text
page
size
sort
propertyId
supplierId
cityId
from
to
status
search
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "property": {
        "id": "uuid",
        "name": "Casa Vista Hermosa"
      },
      "supplier": {
        "id": "uuid",
        "name": "EPA"
      },
      "city": {
        "id": "uuid",
        "name": "Guatemala"
      },
      "purchaseDate": "2026-05-29",
      "status": "OPEN",
      "totalEstimated": 350.00,
      "purchasedItems": 2,
      "totalItems": 5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

## 18.2 Get purchase list by ID

```http
GET /api/v1/purchase-lists/{id}
```

Response:

```json
{
  "id": "uuid",
  "property": {
    "id": "uuid",
    "name": "Casa Vista Hermosa"
  },
  "supplier": {
    "id": "uuid",
    "name": "EPA"
  },
  "city": {
    "id": "uuid",
    "name": "Guatemala"
  },
  "purchaseDate": "2026-05-29",
  "notes": "Monthly supplies",
  "status": "OPEN",
  "items": [
    {
      "id": "uuid",
      "materialId": "uuid",
      "brandId": "uuid",
      "itemName": "Water filter",
      "quantity": 2,
      "unit": "unit",
      "estimatedPrice": 150.00,
      "purchased": false,
      "notes": "Sediment filter"
    }
  ]
}
```

---

## 18.3 Create purchase list

```http
POST /api/v1/purchase-lists
```

Request:

```json
{
  "propertyId": "uuid",
  "supplierId": "uuid",
  "cityId": "uuid",
  "purchaseDate": "2026-05-29",
  "notes": "Monthly supplies",
  "items": [
    {
      "materialId": "uuid",
      "brandId": "uuid",
      "itemName": "Water filter",
      "quantity": 2,
      "unit": "unit",
      "estimatedPrice": 150.00,
      "purchased": false,
      "notes": "Sediment filter"
    }
  ]
}
```

Validaciones:

- `purchaseDate` requerido.
- Items no deben estar vacíos.
- `quantity` mayor a 0.
- `estimatedPrice` no debe ser negativo.
- Entidades relacionadas deben pertenecer a la organización.

Response:

```http
201 Created
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 18.4 Update purchase list

```http
PUT /api/v1/purchase-lists/{id}
```

Request similar a create.

---

## 18.5 Mark purchase item

```http
PATCH /api/v1/purchase-lists/{purchaseListId}/items/{itemId}/purchased
```

Request:

```json
{
  "purchased": true
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF

---

## 18.6 Delete purchase list

```http
DELETE /api/v1/purchase-lists/{id}
```

Comportamiento:

- Soft delete.
- Status `DELETED`.

---

# 19. Documents API

## 19.1 List documents

```http
GET /api/v1/documents
```

Query params:

```text
page
size
sort
propertyId
documentType
processingStatus
status
search
```

Response:

```json
{
  "content": [
    {
      "id": "uuid",
      "property": {
        "id": "uuid",
        "name": "Casa Vista Hermosa"
      },
      "documentType": "HOUSE_RULES",
      "title": "House Rules",
      "originalFilename": "rules.pdf",
      "contentType": "application/pdf",
      "sizeBytes": 245000,
      "processingStatus": "PROCESSED",
      "status": "ACTIVE",
      "createdAt": "2026-05-29T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

## 19.2 Get document by ID

```http
GET /api/v1/documents/{id}
```

Response:

```json
{
  "id": "uuid",
  "property": {
    "id": "uuid",
    "name": "Casa Vista Hermosa"
  },
  "documentType": "HOUSE_RULES",
  "title": "House Rules",
  "description": "Main rules for the property",
  "originalFilename": "rules.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 245000,
  "processingStatus": "PROCESSED",
  "status": "ACTIVE",
  "createdAt": "2026-05-29T12:00:00Z",
  "updatedAt": "2026-05-29T12:00:00Z"
}
```

---

## 19.3 Upload document

```http
POST /api/v1/documents
Content-Type: multipart/form-data
```

Request:

```text
propertyId: uuid
documentType: HOUSE_RULES
title: House Rules
description: Main rules for the property
file: rules.pdf
```

Validaciones:

- `documentType` requerido.
- `title` requerido.
- `file` requerido.
- Si `propertyId` se envía, debe pertenecer a la organización.
- Tipos permitidos:
  - PDF
  - DOCX
  - TXT
  - JPG
  - PNG
  - WEBP
- Tamaño máximo definido por configuración.

Response:

```http
201 Created
```

Response body:

```json
{
  "id": "uuid",
  "documentType": "HOUSE_RULES",
  "title": "House Rules",
  "originalFilename": "rules.pdf",
  "processingStatus": "PENDING",
  "status": "ACTIVE"
}
```

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 19.4 Get document download URL

```http
GET /api/v1/documents/{id}/download-url
```

Response:

```json
{
  "url": "https://s3-presigned-url",
  "expiresIn": 300
}
```

Seguridad:

- Usuarios con permiso de lectura sobre documentos.

---

## 19.5 Process document for AI

```http
POST /api/v1/documents/{id}/process
```

Comportamiento:

- Cambiar estado a `PROCESSING`.
- Extraer texto.
- Dividir en chunks.
- Crear embeddings.
- Guardar embeddings en Chroma.
- Guardar metadatos en `document_chunks`.
- Cambiar estado a `PROCESSED` o `FAILED`.

Response:

```json
{
  "id": "uuid",
  "processingStatus": "PROCESSING"
}
```

MVP:

- Puede ser síncrono al inicio para simplificar.
- Idealmente debería moverse a proceso async en fase posterior.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

## 19.6 Delete document

```http
DELETE /api/v1/documents/{id}
```

Comportamiento:

- Soft delete.
- Status `DELETED`.
- No borrar archivo S3 inmediatamente en MVP.
- Opcionalmente marcar chunks como no disponibles en fase posterior.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER

---

# 20. AI Assistant API

## 20.1 Ask AI Assistant

```http
POST /api/v1/ai/chat
```

Request:

```json
{
  "propertyId": "uuid",
  "message": "¿Qué dice el reglamento sobre mascotas?"
}
```

Validaciones:

- `message` requerido.
- Si `propertyId` se envía, debe pertenecer a la organización.
- El usuario debe tener permiso para consultar documentos de esa propiedad.

Response:

```json
{
  "answer": "Según el reglamento de la propiedad, las mascotas no están permitidas...",
  "sources": [
    {
      "documentId": "uuid",
      "documentTitle": "House Rules",
      "documentType": "HOUSE_RULES",
      "chunkIndex": 3,
      "excerpt": "Pets are not allowed...",
      "score": 0.87
    }
  ],
  "confidence": "HIGH"
}
```

Valores de confidence:

```text
HIGH
MEDIUM
LOW
UNKNOWN
```

Reglas:

- La respuesta debe citar fuentes cuando proviene de documentos.
- Si no hay información suficiente, responder claramente que no se encontró información concluyente.
- No inventar reglas de la propiedad.
- No consultar datos de otras organizaciones.

Seguridad:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF, limitado según permisos.
- READ_ONLY

---

## 20.2 Search documents with AI

```http
POST /api/v1/ai/documents/search
```

Request:

```json
{
  "propertyId": "uuid",
  "query": "mascotas",
  "documentTypes": [
    "HOUSE_RULES"
  ],
  "limit": 5
}
```

Response:

```json
{
  "results": [
    {
      "documentId": "uuid",
      "documentTitle": "House Rules",
      "documentType": "HOUSE_RULES",
      "chunkIndex": 3,
      "excerpt": "Pets are not allowed...",
      "score": 0.87
    }
  ]
}
```

Uso:

- Debug.
- UI avanzada.
- Validación de RAG.

Seguridad:

- Usuarios con permiso de lectura sobre documentos.

---

# 21. Health API

## 21.1 Health check

```http
GET /api/v1/health
```

Response:

```json
{
  "status": "UP",
  "service": "tamias-api",
  "timestamp": "2026-05-29T12:00:00Z"
}
```

Uso:

- Render health checks.
- Monitoreo básico.
- Debug.

Seguridad:

- Público o protegido según despliegue.
- En MVP puede ser público sin exponer información sensible.

---

# 22. DTOs transversales recomendados

## 22.1 IdNameResponse

Para referencias simples:

```json
{
  "id": "uuid",
  "name": "Casa Vista Hermosa"
}
```

Uso:

- property
- category
- type
- platform
- supplier
- city
- material
- brand

---

## 22.2 PageResponse

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

## 22.3 ErrorResponse

```json
{
  "timestamp": "2026-05-29T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/properties",
  "details": []
}
```

---

## 22.4 FileUploadResponse

```json
{
  "id": "uuid",
  "originalFilename": "file.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 245000,
  "url": "https://..."
}
```

---

# 23. Validaciones transversales

## 23.1 UUID

Todo path variable de tipo ID debe ser UUID válido.

Error si no es válido:

```http
400 Bad Request
```

## 23.2 Recurso inexistente

Si el recurso no existe o no pertenece a la organización:

```http
404 Not Found
```

## 23.3 Conflictos de nombre

Si se intenta crear un catálogo con nombre duplicado en la organización:

```http
409 Conflict
```

## 23.4 Estados inválidos

Si se envía un status no permitido:

```http
400 Bad Request
```

## 23.5 Archivos inválidos

Si archivo supera tamaño máximo o tipo no permitido:

```http
400 Bad Request
```

---

# 24. Reglas para Swagger/OpenAPI

Cada controller debe documentar:

- Descripción del endpoint.
- Roles permitidos.
- Request body.
- Response body.
- Códigos de respuesta.
- Query params.
- Ejemplos básicos.

Configuración recomendada:

```text
OpenAPI title: TAMIAS API
Version: v1
Description: REST API for TAMIAS SaaS property operations platform.
Security: Bearer JWT
```

---

# 25. Orden recomendado de implementación

Implementar APIs en este orden:

1. Health
2. Auth
3. Organizations
4. Users
5. Properties
6. Catalogs
7. Maintenance Records
8. Scheduled Maintenance
9. Guests
10. Reservations
11. Task Lists
12. Purchase Lists
13. Documents
14. AI Assistant

Razón:

- Auth y multi-tenancy son base de todo.
- Properties y catalogs desbloquean módulos operativos.
- Maintenance, reservations y purchases dependen de properties/catalogs.
- Documents dependen de properties y S3.
- AI depende de documents.

---

# 26. Endpoints resumen

## Auth

```http
POST /api/v1/auth/login
GET  /api/v1/auth/me
POST /api/v1/auth/logout
```

## Organizations

```http
GET /api/v1/organizations/current
PUT /api/v1/organizations/current
```

## Users

```http
GET    /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

## Properties

```http
GET    /api/v1/properties
GET    /api/v1/properties/{id}
POST   /api/v1/properties
PUT    /api/v1/properties/{id}
DELETE /api/v1/properties/{id}
POST   /api/v1/properties/{id}/images
```

## Catalogs

```http
/api/v1/maintenance-categories
/api/v1/maintenance-types
/api/v1/maintenance-people
/api/v1/platforms
/api/v1/suppliers
/api/v1/cities
/api/v1/materials
/api/v1/brands
/api/v1/task-templates
```

Cada catálogo:

```http
GET
GET /{id}
POST
PUT /{id}
DELETE /{id}
```

## Maintenance

```http
GET    /api/v1/maintenance-records
GET    /api/v1/maintenance-records/{id}
POST   /api/v1/maintenance-records
PUT    /api/v1/maintenance-records/{id}
DELETE /api/v1/maintenance-records/{id}
POST   /api/v1/maintenance-records/{id}/images
```

## Scheduled Maintenance

```http
GET   /api/v1/scheduled-maintenance
GET   /api/v1/scheduled-maintenance/calendar
GET   /api/v1/scheduled-maintenance/{id}
POST  /api/v1/scheduled-maintenance
PUT   /api/v1/scheduled-maintenance/{id}
PATCH /api/v1/scheduled-maintenance/{id}/reschedule
PATCH /api/v1/scheduled-maintenance/{id}/cancel
PATCH /api/v1/scheduled-maintenance/{id}/complete
```

## Reservations

```http
GET    /api/v1/reservations
GET    /api/v1/reservations/{id}
POST   /api/v1/reservations
PUT    /api/v1/reservations/{id}
PATCH  /api/v1/reservations/{id}/cancel
DELETE /api/v1/reservations/{id}
```

## Guests

```http
GET    /api/v1/guests
GET    /api/v1/guests/{id}
POST   /api/v1/guests
PUT    /api/v1/guests/{id}
DELETE /api/v1/guests/{id}
```

## Task Lists

```http
GET    /api/v1/task-lists
GET    /api/v1/task-lists/{id}
POST   /api/v1/task-lists
PUT    /api/v1/task-lists/{id}
PATCH  /api/v1/task-lists/{taskListId}/items/{itemId}/complete
DELETE /api/v1/task-lists/{id}
```

## Purchase Lists

```http
GET    /api/v1/purchase-lists
GET    /api/v1/purchase-lists/{id}
POST   /api/v1/purchase-lists
PUT    /api/v1/purchase-lists/{id}
PATCH  /api/v1/purchase-lists/{purchaseListId}/items/{itemId}/purchased
DELETE /api/v1/purchase-lists/{id}
```

## Documents

```http
GET    /api/v1/documents
GET    /api/v1/documents/{id}
POST   /api/v1/documents
GET    /api/v1/documents/{id}/download-url
POST   /api/v1/documents/{id}/process
DELETE /api/v1/documents/{id}
```

## AI

```http
POST /api/v1/ai/chat
POST /api/v1/ai/documents/search
```

## Health

```http
GET /api/v1/health
```

---

# 27. Decisiones abiertas

## 27.1 Refresh tokens

MVP:

- Solo access token.
- Refresh token puede quedar para fase posterior.

## 27.2 Permisos granulares

MVP:

- Seguridad por rol.

Futuro:

- Permisos por acción y módulo.

## 27.3 Procesamiento async de documentos

MVP:

- Puede ser síncrono para simplificar.

Futuro:

- Procesamiento async con cola o job.

## 27.4 Cambio de organización activa

La base soporta múltiples organizaciones por usuario.

MVP:

- Usar una organización activa por usuario.
- Selector de organización puede quedar para fase posterior.

---

# 28. Próximo entregable recomendado

Después de este documento, el siguiente entregable recomendado es:

```text
TAMIAS — Diseño de Backend Spring Boot MVP
```

Archivo sugerido:

```text
docs/04-backend-design-mvp.md
```

Ese documento debe definir:

- Estructura de paquetes.
- Capas.
- Entidades base.
- DTOs.
- Mappers.
- Repositories.
- Services.
- Controllers.
- Seguridad JWT.
- Manejo de errores.
- Validación multi-tenant.
- Configuración Flyway.
- Configuración Swagger/OpenAPI.
- Estrategia de tests.
