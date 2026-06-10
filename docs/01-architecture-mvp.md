# TAMIAS — Arquitectura y alcance MVP

## 1. Visión del producto

TAMIAS es una plataforma SaaS para la administración operativa de alojamientos pequeños como casas vacacionales, apartamentos, bungalows, cabañas y villas.

El objetivo principal es ayudar a propietarios y administradores a controlar desde una sola plataforma:

- Propiedades.
- Mantenimiento.
- Mantenimiento programado.
- Reservaciones.
- Supplies entregados en reservaciones.
- Tareas.
- Compras.
- Inventory Items.
- Documentos.
- Usuarios y roles.
- Perfil de usuario.
- Cambio obligatorio de contraseña temporal.
- Búsqueda documental con IA.
- Dashboard operativo.

TAMIAS no es únicamente un sistema CRUD. Su diferenciador es combinar administración operativa con un asistente de IA capaz de consultar documentos, responder preguntas del negocio y, en fases posteriores, consultar datos operativos mediante tool calling controlado.

---

## 2. Objetivo profesional

TAMIAS también es una pieza principal del portfolio profesional de Juan Tzun.

El proyecto debe demostrar:

- Buen diseño de arquitectura.
- Backend profesional con Java 21 y Spring Boot 3.
- Frontend administrativo moderno con Angular.
- Seguridad con JWT, roles y permisos.
- Administración de usuarios.
- Self-service profile.
- Cambio obligatorio de contraseña temporal.
- Modelo SaaS multi-tenant.
- Manejo de archivos en AWS S3.
- Base de datos PostgreSQL con migraciones Flyway.
- Uso real de IA con RAG, embeddings, Chroma, Spring AI y OpenAI.
- DevOps con Docker, GitHub Actions y despliegue real.
- Documentación técnica clara.

---

## 3. Arquitectura recomendada

TAMIAS inicia como un **Modular Monolith** usando Spring Boot 3.

Arquitectura general:

```text
Angular Frontend
        |
        | REST API + JWT
        v
Spring Boot Backend
        |
        | JPA / Hibernate
        v
PostgreSQL

Spring Boot Backend
        |
        | AWS SDK
        v
AWS S3

Spring Boot Backend
        |
        | Spring AI
        v
OpenAI / Chroma / Ollama
```

---

## 4. Modelo SaaS y multi-tenant

TAMIAS usa:

```text
Shared database + shared schema + organization_id
```

Esto significa que todas las organizaciones comparten la misma base de datos y las mismas tablas, pero los registros se separan mediante `organization_id`.

Regla crítica:

> Toda consulta de datos operativos debe filtrar por el `organization_id` del usuario autenticado.

El frontend no debe enviar `organization_id` como fuente confiable. El backend debe resolverlo a partir del usuario autenticado y su contexto de seguridad.

---

## 5. Alcance MVP actualizado

### Módulos incluidos en el MVP actual

- Authentication.
- Organizations.
- Users.
- User Management.
- Roles básicos.
- My Profile.
- Mandatory Temporary Password Change.
- Properties.
- Property Images.
- Catalogs.
- Inventory Items.
- Maintenance.
- Maintenance Record Items.
- Maintenance Images.
- Scheduled Maintenance.
- Scheduled Maintenance History.
- Reservations.
- Guests.
- Reservation Supplies.
- Task Lists.
- Purchase Lists.
- Purchase Items.
- Documents.
- AWS S3 File Storage.
- AI Document Search con RAG.
- AI Chat Sessions.
- Dashboard Calendar.
- Basic Deploy foundation.

### Módulos fuera del MVP inicial

- Recuperación de contraseña por correo.
- Invitaciones por correo.
- Permisos personalizados dinámicos.
- JasperReports avanzados.
- Reportes complejos.
- Tool Calling completo contra PostgreSQL.
- Blueprint Analysis con OCR/Vision.
- AI Agents especializados.
- Billing/subscriptions.
- Integraciones directas con Airbnb, Booking o VRBO.
- Inventario formal con stock y movimientos.
- Notificaciones automáticas avanzadas.

---

## 6. Seguridad, usuarios y perfiles

TAMIAS separa claramente dos responsabilidades:

### User Management

Ruta frontend:

```text
/users
```

Backend:

```text
/api/v1/users
```

Uso:

- Solo administradores.
- Crear usuarios.
- Asignar roles.
- Actualizar estado.
- Activar/desactivar usuarios.
- Eliminar usuarios.

Cuando un administrador crea un usuario, el password inicial se considera temporal.

### My Profile

Ruta frontend:

```text
/profile
```

Backend:

```text
/api/v1/profile
```

Uso:

- Disponible para todo usuario autenticado.
- Actualizar `firstName`.
- Actualizar `lastName`.
- Cambiar su propio password.

No requiere rol `ADMINISTRATOR`.

### Mandatory Temporary Password Change

Campo de base de datos:

```text
users.password_change_required
```

Flujo:

```text
Admin crea usuario
        |
Backend guarda password temporal
        |
password_change_required = true
        |
Usuario inicia sesión
        |
Frontend detecta passwordChangeRequired
        |
Usuario es enviado a /profile
        |
No puede continuar usando el sistema hasta cambiar password
        |
Backend guarda nuevo password
        |
password_change_required = false
```

---

## 7. Roles iniciales

### Administrator

Puede administrar toda la organización:

- Usuarios.
- Roles.
- Propiedades.
- Catálogos.
- Inventory Items.
- Mantenimientos.
- Reservaciones.
- Supplies.
- Compras.
- Documentos.
- IA.
- Reportes.

### Property Manager

Puede gestionar la operación diaria:

- Propiedades asignadas.
- Mantenimientos.
- Reservaciones.
- Supplies de reservaciones.
- Compras.
- Documentos.
- Consultas operativas.

No debe administrar usuarios ni configuraciones críticas.

### Maintenance Staff

Puede:

- Ver mantenimientos asignados.
- Completar tareas.
- Subir evidencia.
- Agregar observaciones.
- Consultar datos operativos limitados.

No debe ver información financiera completa ni administrar catálogos críticos.

### Read Only

Puede consultar información, pero no modificarla.

### Todos los roles autenticados

Pueden acceder a:

```text
/profile
```

para actualizar sus datos personales y cambiar su password.

---

## 8. Módulos backend

Estructura sugerida:

```text
com.tamias
  config
  security
  common
  organization
  user
  profile
  property
  catalog
  maintenance
  reservation
  task
  purchase
  document
  ai
  notification
  report
```

Cada módulo puede organizarse internamente así:

```text
controller
service
repository
entity
dto
mapper
exception
```

---

## 9. Entidades principales del MVP

### Seguridad y organización

- Organization.
- User.
- Role.
- UserOrganization.

Campo importante en `User`:

```text
password_change_required
```

### Propiedades

- Property.
- PropertyImage.

### Catálogos

- MaintenanceCategory.
- MaintenanceType.
- MaintenancePerson.
- Platform.
- Supplier.
- City.
- Brand.
- TaskTemplate.
- InventoryItem.

### Mantenimiento

- MaintenanceRecord.
- MaintenanceRecordImage.
- MaintenanceRecordItem.
- ScheduledMaintenance.
- ScheduledMaintenanceHistory.

### Reservaciones

- Reservation.
- Guest.
- ReservationGuest.
- ReservationSupply.

### Tareas

- TaskList.
- TaskItem.

### Compras

- PurchaseList.
- PurchaseItem.

### Documentos

- Document.
- DocumentChunkMetadata.

### IA

- AiChatSession.
- AiChatMessage.

---

## 10. API REST

Las rutas deben ser versionadas:

```text
/api/v1/auth
/api/v1/profile
/api/v1/users
/api/v1/organizations
/api/v1/properties
/api/v1/catalogs
/api/v1/inventory-items
/api/v1/maintenance-records
/api/v1/scheduled-maintenance
/api/v1/reservations
/api/v1/purchase-lists
/api/v1/documents
/api/v1/ai
```

Endpoints de perfil:

```http
GET   /api/v1/profile
PATCH /api/v1/profile
PATCH /api/v1/profile/password
```

---

## 11. Frontend Angular

Estructura recomendada:

```text
src/app
  core
  shared
  features
    auth
    dashboard
    profile
    properties
    catalogs
    maintenance
    scheduled-maintenance
    reservations
    tasks
    purchases
    documents
    ai-assistant
    users
```

Pantallas MVP:

- Login.
- Dashboard.
- My Profile.
- Properties.
- Catalogs.
- Inventory Items.
- Maintenance Records.
- Scheduled Maintenance.
- Reservations.
- Reservation Supplies modal.
- Task Lists.
- Purchase Lists.
- Documents.
- AI Assistant.
- Users.

---

## 12. Seguridad

Principios:

- JWT para autenticación.
- Roles y permisos validados en backend.
- Multi-tenant enforced por `organization_id`.
- No confiar en IDs enviados por frontend para aislamiento de datos.
- `/users` restringido a administradores.
- `/profile` disponible para cualquier usuario autenticado.
- Password temporal debe obligar cambio en primer login.
- El cambio de password debe requerir password actual.
- Pre-signed URLs para archivos S3.
- Validación de tamaño y tipo de archivo.
- Soft delete para entidades críticas.
- Auditoría básica con `created_at`, `updated_at`, `created_by`, `updated_by`.
