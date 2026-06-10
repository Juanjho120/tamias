# TAMIAS — Diseño de Frontend Angular MVP

Este documento define el diseño técnico del frontend para el MVP de TAMIAS.

---

## 1. Objetivo

El frontend de TAMIAS proporciona una interfaz web administrativa para que propietarios y administradores de alojamientos pequeños puedan controlar su operación diaria.

Debe permitir administrar:

- Autenticación.
- Dashboard.
- Propiedades.
- Catálogos.
- Inventory Items.
- Mantenimientos.
- Mantenimientos programados.
- Calendario.
- Reservaciones.
- Reservation Supplies.
- Tareas.
- Compras.
- Documentos.
- Asistente IA.
- Usuarios.

---

## 2. Stack frontend

```text
Angular
TypeScript
Bootstrap
Bootstrap Icons
Angular Reactive Forms
FullCalendar
RxJS
Angular Router
HTTP Client
ngx-translate
```

Recomendaciones:

- Usar Angular standalone components.
- Usar TypeScript estricto.
- Usar Reactive Forms para formularios.
- Usar Bootstrap para acelerar UI.
- Usar FullCalendar para calendario/dashboard.
- Evitar librerías UI pesadas al inicio.

---

## 3. Arquitectura frontend

El frontend se organiza por features:

```text
core
shared
features
```

Principios:

- `core` contiene servicios globales y configuración.
- `shared` contiene componentes reutilizables.
- `features` contiene módulos/pantallas del dominio.
- Cada feature encapsula páginas, componentes, modelos y servicios.
- No duplicar lógica entre features.
- No llamar APIs directamente desde componentes si la lógica puede vivir en services.

---

## 4. Estructura principal

```text
frontend/
  src/
    app/
      app.config.ts
      app.routes.ts
      core/
      shared/
      features/
        auth/
        dashboard/
        properties/
        catalogs/
        maintenance/
        scheduled-maintenance/
        reservations/
        tasks/
        purchases/
        documents/
        ai-assistant/
        users/
    assets/
  public/
    assets/
      i18n/
    brand/
  angular.json
  package.json
```

---

## 5. Rutas principales

```text
/login
/dashboard
/properties
/catalogs
/maintenance-records
/scheduled-maintenance
/reservations
/task-lists
/purchase-lists
/documents
/ai-assistant
/users
```

Inventory Items se administra desde la sección Catalogs, usando el endpoint backend:

```text
/api/v1/inventory-items
```

---

## 6. Layout

### Auth Layout

Usado para:

```text
/login
```

Debe contener:

- Logo oficial de TAMIAS.
- Formulario de login.
- Mensajes de error.
- Diseño responsive.

### Main Layout

Usado para rutas autenticadas.

Debe contener:

- Sidebar.
- Topbar.
- Área principal.
- Nombre de organización.
- Usuario actual.
- Botón de logout.
- Navegación principal.

---

## 7. Navegación principal

Menú recomendado:

```text
Dashboard
Properties
Maintenance
Scheduled Maintenance
Reservations
Tasks
Purchase Lists
Documents
AI Assistant
Catalogs
Users
```

---

## 8. Features actuales

### Auth

- Login.
- JWT storage.
- Auth guard.
- Interceptor HTTP.

### Dashboard

- Cards resumen.
- Calendario operativo.
- Reservaciones.
- Mantenimientos.
- Mantenimientos programados.
- Tareas.
- Compras.
- Próximamente: analítica más profunda.

### Catalogs

Incluye catálogos operativos:

- Maintenance Categories.
- Maintenance Types.
- Maintenance People.
- Platforms.
- Suppliers.
- Cities.
- Brands.
- Task Templates.
- Inventory Items.

### Inventory Items

UI label recomendado:

```text
Spanish: Insumos y materiales
English: Inventory Items
```

Campos principales:

- Name.
- Description.
- Unit.
- Item Type.
- Internal Code.
- Barcode.
- Available for Maintenance.
- Available for Reservations.
- Available for Purchases.
- Status.

### Maintenance

Incluye:

- Maintenance records.
- Maintenance details.
- Maintenance record items.
- Images.
- People involved.
- Costs.

Los items de mantenimiento se manejan como:

```text
MaintenanceRecordItem
```

y se relacionan con:

```text
InventoryItem
```

### Reservations

Incluye:

- Reservation list.
- Create/edit reservation modal.
- Guests.
- Reservation cancellation.
- Calendar/dashboard integration.

### Reservation Supplies

Reservation supplies se maneja en modal separado, abierto desde la tabla de reservaciones con el botón:

```text
Supplies
```

El modal permite:

- Listar supplies de una reservación.
- Agregar supply.
- Editar supply.
- Eliminar supply.

Los supplies usan `InventoryItem` filtrados por:

```text
availableForReservations = true
```

### Purchases

Incluye:

- Purchase lists.
- Purchase items.
- Modal principal de lista de compras.
- Modal separado de items.
- Toggle purchased.
- Inventory Items disponibles para compras.

### Documents

Incluye:

- Upload.
- AWS S3.
- Pre-signed URLs.
- Processing status.
- Document metadata.
- RAG processing.

### AI Assistant

Incluye:

- Chat UI.
- Sessions.
- RAG over documents.
- Answers grounded in document chunks.

---

## 9. Servicios HTTP por feature

Cada feature debe tener su propio service.

Ejemplos:

```text
PropertyService
CatalogService
InventoryItemService
MaintenanceDetailService
ScheduledMaintenanceService
ReservationService
PurchaseListService
DocumentService
AiAssistantService
UserService
```

---

## 10. i18n

El frontend usa archivos JSON bajo:

```text
public/assets/i18n/
```

Idiomas iniciales:

- English.
- Spanish.

Reglas:

- No hardcodear textos visibles cuando ya exista infraestructura i18n.
- Mantener keys agrupadas por feature.
- Evitar keys legacy después de refactors.

---

## 11. Formularios reactivos

Todos los formularios principales deben usar Reactive Forms.

Reglas:

- Validación frontend para UX.
- Validación backend como fuente real de seguridad.
- No confiar en valores enviados por frontend para multi-tenancy.
- Mostrar mensajes claros.
- Manejar loading/saving states.

---

## 12. Manejo de errores

El frontend debe:

- Capturar errores HTTP con interceptor.
- Mostrar toast/error UX.
- Mostrar validaciones de campo.
- No mostrar stack traces.
- Manejar `401` redirigiendo a login.
- Manejar `403` mostrando mensaje de permisos.

---

## 13. Responsive design

El MVP debe verse correctamente en:

- Desktop.
- Laptop.
- Tablet.
- Mobile básico.

No se requiere diseño mobile-first completo, pero los modales y tablas deben ser utilizables.

---

## 14. Próximos pasos frontend

Prioridades siguientes:

1. MVP hardening.
2. Mejorar empty states.
3. Mejorar loading states.
4. Dashboard analytics.
5. Mejorar tests de servicios/componentes.
6. Preparar producción.
