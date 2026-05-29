# TAMIAS — Documento inicial de arquitectura y alcance MVP

## 1. Visión del producto

TAMIAS será una plataforma SaaS para la administración operativa de alojamientos pequeños como casas vacacionales, apartamentos, bungalows, cabañas y villas.

El objetivo principal es ayudar a propietarios y administradores a controlar mantenimiento, reservaciones, tareas, compras, documentos importantes, reportes y operaciones generales desde una sola plataforma.

TAMIAS no será únicamente un sistema CRUD. Su diferenciador será combinar administración operativa con un asistente de IA capaz de consultar documentos, responder preguntas del negocio y, en fases posteriores, analizar planos y ejecutar consultas controladas mediante tool calling.

---

## 2. Objetivo profesional

TAMIAS también será una pieza principal del portfolio profesional de Juan Tzun.

Por eso, el proyecto debe demostrar:

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

- Angular
- TypeScript
- Bootstrap
- Angular Reactive Forms
- FullCalendar

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT Authentication
- Spring Data JPA
- Hibernate
- Flyway
- Swagger/OpenAPI

### Base de datos

- PostgreSQL

### Archivos

- AWS S3

### Reportería

- JasperReports
- iReport

### Correos

- Java Mail Sender

### Inteligencia Artificial

- Spring AI
- OpenAI
- Ollama
- Chroma
- RAG
- Tool Calling
- AI Agents

### DevOps

- Docker
- Docker Compose
- GitHub Actions
- CI/CD

### Despliegue

- Frontend: Vercel
- Backend: Render
- Base de datos: Supabase PostgreSQL
- IA: Railway
- Archivos: AWS S3
- Dominio: tamias.juantzun.dev

---

## 4. Arquitectura recomendada

TAMIAS iniciará como un Modular Monolith usando Spring Boot 3.

No se recomienda iniciar con microservicios porque:

- El producto todavía está en etapa inicial.
- Cada organización tendrá aproximadamente hasta 5 usuarios simultáneos.
- Microservicios aumentarían la complejidad de despliegue, observabilidad, seguridad y comunicación interna.
- Para portfolio, es más valioso demostrar una arquitectura modular, limpia y bien documentada que una arquitectura distribuida innecesaria.

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

TAMIAS debe nacer con estructura multi-tenant.

La entidad raíz será:

```text
Organization
```

Cada organización representa a un propietario, administrador o empresa que administra alojamientos.

La estrategia multi-tenant será:

```text
Shared database + shared schema + organization_id
```

Esto significa que todas las organizaciones comparten la misma base de datos y las mismas tablas, pero los registros se separan mediante `organization_id`.

Ejemplo:

```sql
properties
- id
- organization_id
- name
- address
- description
- status
```

Regla crítica:

> Toda consulta de datos operativos debe filtrar por el `organization_id` del usuario autenticado.

El frontend no debe enviar el `organization_id` como fuente confiable. El backend debe resolverlo a partir del usuario autenticado y su contexto de seguridad.

---

## 6. Alcance MVP

El MVP debe enfocarse en construir una base real, funcional y presentable.

### Módulos incluidos en MVP

- Authentication
- Organizations
- Users
- Roles básicos
- Properties
- Catalogs
- Maintenance
- Scheduled Maintenance
- Reservations
- Purchase Lists
- Documents
- AI Document Search con RAG
- Basic Deploy

### Módulos fuera del MVP inicial

Estos módulos quedan para versiones posteriores:

- Recuperación de contraseña.
- Invitaciones por correo.
- JasperReports avanzados.
- Reportes complejos.
- Tool Calling completo contra PostgreSQL.
- Blueprint Analysis con OCR/Vision.
- AI Agents especializados.
- Billing/subscriptions.
- Integraciones directas con Airbnb, Booking o VRBO.
- Inventario formal.
- Notificaciones automáticas avanzadas.

---

## 7. Roles iniciales

Los roles iniciales son:

### Administrator

Puede administrar toda la organización:

- Usuarios.
- Roles.
- Propiedades.
- Catálogos.
- Mantenimientos.
- Reservaciones.
- Compras.
- Documentos.
- IA.
- Reportes.

### Property Manager

Puede gestionar la operación diaria:

- Propiedades asignadas.
- Mantenimientos.
- Reservaciones.
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
  purchase
  document
  notification
  report
  ai
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

Ejemplo:

```text
maintenance
  controller
  service
  repository
  entity
  dto
  mapper
```

---

## 9. Entidades principales del MVP

### Seguridad y organización

- Organization
- User
- Role
- UserOrganization

### Propiedades

- Property
- PropertyImage

### Catálogos

- MaintenanceCategory
- MaintenanceType
- MaintenancePerson
- Platform
- Supplier
- City
- Material
- Supply
- TaskTemplate
- Brand

### Mantenimiento

- MaintenanceRecord
- MaintenanceRecordImage
- MaintenanceMaterialUsed
- ScheduledMaintenance
- ScheduledMaintenanceHistory

### Reservaciones

- Reservation
- Guest
- ReservationGuest

### Compras

- PurchaseList
- PurchaseItem

### Documentos

- Document
- DocumentChunkMetadata

Los embeddings se guardarán en Chroma, mientras PostgreSQL mantendrá metadatos, relaciones y trazabilidad.

---

## 10. API REST

Las rutas deben ser versionadas:

```text
/api/v1/auth
/api/v1/users
/api/v1/organizations
/api/v1/properties
/api/v1/catalogs
/api/v1/maintenance-records
/api/v1/scheduled-maintenance
/api/v1/reservations
/api/v1/purchase-lists
/api/v1/documents
/api/v1/ai
```

Ejemplo de propiedades:

```http
GET    /api/v1/properties
GET    /api/v1/properties/{id}
POST   /api/v1/properties
PUT    /api/v1/properties/{id}
DELETE /api/v1/properties/{id}
```

Ejemplo de documentos:

```http
POST   /api/v1/documents
GET    /api/v1/documents
GET    /api/v1/documents/{id}
DELETE /api/v1/documents/{id}
POST   /api/v1/documents/{id}/process
```

Ejemplo de IA:

```http
POST /api/v1/ai/chat
POST /api/v1/ai/documents/search
```

---

## 11. Frontend Angular

Estructura recomendada:

```text
src/app
  core
    auth
    interceptors
    guards
    layout
  shared
    components
    pipes
    validators
  features
    dashboard
    properties
    maintenance
    reservations
    purchases
    documents
    ai-assistant
    catalogs
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
- FullCalendar para mantenimientos programados.

Pantallas MVP:

- Login
- Dashboard
- Properties
- Property Form
- Maintenance Records
- Maintenance Form
- Scheduled Maintenance
- Maintenance Calendar
- Reservations
- Purchase Lists
- Documents
- AI Assistant
- Catalogs

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

Regla importante:

> La IA no debe ejecutar SQL libre directamente.

Se deben exponer herramientas específicas:

```text
findLastPurchaseByMaterial(materialName)
getMaintenanceCostByYear(year)
findOverdueTasks()
findLastMaintenanceByCategory(categoryName)
```

### Fase IA 3 — Blueprint Analysis

Objetivo:

Analizar planos con OCR y modelos de visión.

Ejemplos:

- ¿Cuánto mide la habitación principal?
- ¿Dónde está ubicada la cisterna?
- ¿Qué área tiene la terraza?

Regla:

> Si la información no es concluyente, la IA debe decirlo claramente.

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

Los reportes con JasperReports/iReport quedan fuera del MVP inicial, pero la arquitectura debe dejar espacio para el módulo `report`.

Reportes futuros:

- Maintenance History
- Maintenance Costs
- Upcoming Maintenance
- Reservation Summary
- Purchase History
- Expense Summary
- Inventory Usage
- Task Completion

Ruta futura de ejemplo:

```http
GET /api/v1/reports/maintenance-history?propertyId={id}&from={date}&to={date}
```

---

## 15. Notificaciones

Para fases posteriores:

- Reservación creada.
- Mantenimiento próximo.
- Tarea vencida.
- Invitación creada.
- Mantenimiento reprogramado.

Componentes sugeridos:

- NotificationService
- EmailService
- EmailTemplateService

---

## 16. DevOps y despliegue

### Desarrollo local

Docker Compose debe levantar:

- PostgreSQL
- Chroma
- Ollama, opcional
- Backend
- Frontend

### CI/CD

Pipeline general:

```text
Push / Pull Request
      |
Backend tests
      |
Frontend tests
      |
Backend build
      |
Frontend build
      |
Docker build
      |
Deploy on main
```

### Plataformas

- Frontend: Vercel
- Backend: Render
- PostgreSQL: Supabase
- Vector DB / IA: Railway
- Archivos: AWS S3
- Dominio: tamias.juantzun.dev

---

## 17. Reglas de consistencia del proyecto

Antes de diseñar o implementar cualquier módulo, validar:

1. ¿Respeta el MVP?
2. ¿Respeta el modelo multi-tenant?
3. ¿Filtra por `organization_id` cuando corresponde?
4. ¿Pertenece a la fase actual o futura?
5. ¿Agrega complejidad innecesaria?
6. ¿Contradice una decisión previa?
7. ¿Es útil para portfolio profesional?

---

## 18. Siguiente paso técnico

El siguiente entregable recomendado es:

```text
TAMIAS — Diseño de base de datos MVP
```

Debe incluir:

- Tablas.
- Campos.
- Tipos de datos.
- Relaciones.
- Índices.
- Constraints.
- Migraciones Flyway.
- Decisiones de soft delete.
- Qué entra en MVP y qué queda para fases futuras.
