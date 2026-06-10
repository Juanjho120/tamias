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
- Modelo SaaS multi-tenant.
- Manejo de archivos en AWS S3.
- Base de datos PostgreSQL con migraciones Flyway.
- Uso real de IA con RAG, embeddings, Chroma, Spring AI y OpenAI.
- DevOps con Docker, GitHub Actions y despliegue real.
- Documentación técnica clara.

---

## 3. Stack tecnológico definido

### Frontend

- Angular.
- TypeScript.
- Bootstrap.
- Bootstrap Icons.
- Angular Reactive Forms.
- FullCalendar.
- RxJS.
- ngx-translate.

### Backend

- Java 21.
- Spring Boot 3.
- Spring Security.
- JWT Authentication.
- Spring Data JPA.
- Hibernate.
- Flyway.
- Swagger/OpenAPI.

### Base de datos

- PostgreSQL.

### Archivos

- AWS S3.
- Pre-signed URLs.

### Inteligencia Artificial

- Spring AI.
- OpenAI.
- Ollama, opcional para pruebas locales.
- Chroma.
- RAG.
- Tool Calling, fase futura.
- AI Agents, fase futura.

### DevOps

- Docker.
- Docker Compose.
- GitHub Actions.
- CI/CD.

### Despliegue

- Frontend: Vercel.
- Backend: Render.
- Base de datos: Supabase PostgreSQL.
- IA / Chroma: Railway.
- Archivos: AWS S3.
- Dominio: `tamias.juantzun.dev`.

---

## 4. Arquitectura recomendada

TAMIAS inicia como un **Modular Monolith** usando Spring Boot 3.

No se recomienda iniciar con microservicios porque:

- El producto todavía está en etapa inicial.
- Cada organización tendrá aproximadamente hasta 5 usuarios simultáneos.
- Microservicios aumentarían la complejidad de despliegue, observabilidad, seguridad y comunicación interna.
- Para portfolio, es más valioso demostrar una arquitectura modular, limpia y bien documentada.

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

## 5. Modelo SaaS y multi-tenant

TAMIAS usa:

```text
Shared database + shared schema + organization_id
```

Esto significa que todas las organizaciones comparten la misma base de datos y las mismas tablas, pero los registros se separan mediante `organization_id`.

Regla crítica:

> Toda consulta de datos operativos debe filtrar por el `organization_id` del usuario autenticado.

El frontend no debe enviar `organization_id` como fuente confiable. El backend debe resolverlo a partir del usuario autenticado y su contexto de seguridad.

---

## 6. Alcance MVP actualizado

### Módulos incluidos en el MVP actual

- Authentication.
- Organizations.
- Users.
- Roles básicos.
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

- Recuperación de contraseña.
- Invitaciones por correo.
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

Los embeddings se guardan en Chroma, mientras PostgreSQL mantiene metadatos, relaciones y trazabilidad.

---

## 10. API REST

Las rutas deben ser versionadas:

```text
/api/v1/auth
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

Endpoints operativos relevantes:

```http
GET    /api/v1/inventory-items
POST   /api/v1/inventory-items
PUT    /api/v1/inventory-items/{id}
DELETE /api/v1/inventory-items/{id}
```

```http
GET    /api/v1/maintenance-records/{id}/items
POST   /api/v1/maintenance-records/{id}/items
PUT    /api/v1/maintenance-records/{id}/items/{itemId}
DELETE /api/v1/maintenance-records/{id}/items/{itemId}
```

```http
GET    /api/v1/reservations/{id}/supplies
POST   /api/v1/reservations/{id}/supplies
PUT    /api/v1/reservations/{id}/supplies/{supplyId}
DELETE /api/v1/reservations/{id}/supplies/{supplyId}
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

Principios:

- Angular standalone components.
- Reactive Forms.
- Servicios por feature.
- Interceptor JWT.
- Guards por autenticación y rol.
- Componentes reutilizables.
- Bootstrap para acelerar interfaz.
- FullCalendar para calendario/dashboard.

Pantallas MVP:

- Login.
- Dashboard.
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

## 12. IA en TAMIAS

### Fase IA 1 — RAG sobre documentos

Objetivo:

Permitir preguntas como:

- ¿Qué dice el reglamento sobre mascotas?
- ¿Se permite fumar?
- ¿Qué no está permitido en la propiedad?
- ¿Dónde está ubicado el tablero eléctrico?

Flujo:

```text
Usuario sube documento
        |
Archivo se guarda en S3
        |
Backend extrae texto
        |
Texto se divide en chunks
        |
Chunks se vectorizan
        |
Embeddings se guardan en Chroma
        |
Usuario pregunta
        |
Se recuperan chunks relevantes
        |
OpenAI genera respuesta
        |
Respuesta cita documento fuente
```

### Fase IA 2 — Tool Calling

Objetivo:

Permitir que el asistente consulte datos reales del sistema usando herramientas controladas.

Ejemplos:

- ¿Cuándo compré por última vez filtros de agua?
- ¿Cuánto gasté en mantenimiento este año?
- ¿Qué tareas están vencidas?
- ¿Cuál fue el último mantenimiento de la bomba?
- ¿Qué supplies se entregaron en una reservación específica?

Regla importante:

> La IA no debe ejecutar SQL libre directamente.

Se deben exponer herramientas específicas:

```text
findLastPurchaseByInventoryItem(itemName)
getMaintenanceCostByYear(year)
findOverdueTasks()
findLastMaintenanceByCategory(categoryName)
getReservationSuppliesByReservation(reservationCode)
```

---

## 13. Seguridad

Principios:

- JWT para autenticación.
- Roles y permisos validados en backend.
- Multi-tenant enforced por `organization_id`.
- No confiar en IDs enviados por frontend para aislamiento de datos.
- Pre-signed URLs para archivos S3.
- Validación de tamaño y tipo de archivo.
- Soft delete para entidades críticas.
- Auditoría básica con `created_at`, `updated_at`, `created_by`, `updated_by`.

---

## 14. Reportería

Los reportes con JasperReports/iReport quedan fuera del MVP inicial, pero la arquitectura deja espacio para el módulo `report`.

Reportes futuros:

- Maintenance History.
- Maintenance Costs.
- Upcoming Maintenance.
- Reservation Summary.
- Purchase History.
- Expense Summary.
- Inventory Usage.
- Reservation Supplies Usage.
- Task Completion.

Ruta futura de ejemplo:

```http
GET /api/v1/reports/maintenance-history?propertyId={id}&from={date}&to={date}
```

---

## 15. Reglas de consistencia del proyecto

Antes de diseñar o implementar cualquier módulo, validar:

1. ¿Respeta el MVP?
2. ¿Respeta el modelo multi-tenant?
3. ¿Filtra por `organization_id` cuando corresponde?
4. ¿Pertenece a la fase actual o futura?
5. ¿Agrega complejidad innecesaria?
6. ¿Contradice una decisión previa?
7. ¿Es útil para portfolio profesional?
