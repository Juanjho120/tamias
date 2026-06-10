# TAMIAS — Diseño de Frontend Angular MVP

Este documento define el diseño técnico del frontend para el MVP de TAMIAS.

---

## 1. Objetivo

El frontend de TAMIAS proporciona una interfaz web administrativa para que propietarios y administradores de alojamientos pequeños puedan controlar su operación diaria.

Debe permitir administrar:

- Autenticación.
- Dashboard.
- Perfil de usuario.
- Administración de usuarios.
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

---

## 3. Arquitectura frontend

El frontend se organiza por features:

```text
core
shared
features
```

Principios:

- `core` contiene servicios globales, guards, interceptors y modelos de sesión.
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
        guards/
        interceptors/
        models/
        services/
      shared/
      features/
        auth/
        dashboard/
        profile/
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
/profile
/properties
/catalogs
/maintenance
/scheduled-maintenance
/reservations
/tasks
/purchases
/documents
/ai-assistant
/users
```

`/profile` está disponible para cualquier usuario autenticado.

`/users` debe mostrarse solo a usuarios con rol:

```text
ADMINISTRATOR
```

---

## 6. Auth Flow

Login:

```text
User submits credentials
        |
Backend validates credentials
        |
Backend returns JWT + current user
        |
Frontend stores token and user session
        |
If passwordChangeRequired = true -> redirect to /profile
Otherwise -> redirect to /dashboard
```

---

## 7. Mandatory Password Change Flow

El frontend debe usar el valor:

```text
passwordChangeRequired
```

del usuario autenticado.

Reglas:

- Si `passwordChangeRequired = true`, el usuario debe ser redirigido a `/profile`.
- Mientras `passwordChangeRequired = true`, el usuario no debe poder navegar al resto del sistema.
- `/profile` debe permitir cambiar password usando password actual.
- Cuando el backend responde con `passwordChangeRequired = false`, la sesión local debe actualizarse.
- Después del cambio exitoso, el usuario puede continuar al dashboard.

Guard recomendado:

```text
passwordChangeGuard
```

---

## 8. Layout

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
- Acceso a My Profile.
- Acceso a Users solo si el usuario es Administrator.

---

## 9. Features actuales

### Auth

- Login.
- JWT storage.
- Auth guard.
- Guest guard.
- Password change guard.
- Interceptor HTTP.

### Profile

Ruta:

```text
/profile
```

Permite:

- Ver datos del usuario autenticado.
- Actualizar first name.
- Actualizar last name.
- Cambiar password.

No permite:

- Cambiar email.
- Cambiar rol.
- Cambiar estado.
- Administrar otros usuarios.

### Users

Ruta:

```text
/users
```

Permite a administradores:

- Listar usuarios.
- Crear usuarios.
- Asignar roles.
- Cambiar estado.
- Activar/desactivar.
- Eliminar.

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

### Maintenance

Incluye:

- Maintenance records.
- Maintenance details.
- Maintenance record items.
- Images.
- People involved.
- Costs.

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

## 10. i18n

El frontend usa archivos JSON bajo:

```text
public/assets/i18n/
```

Idiomas iniciales:

- English.
- Spanish.

Keys agregadas por Profile:

```text
navigation.profile
profile.*
```

Reglas:

- No hardcodear textos visibles cuando ya exista infraestructura i18n.
- Mantener keys agrupadas por feature.
- Evitar keys legacy después de refactors.

---

## 11. Manejo de errores

El frontend debe:

- Capturar errores HTTP con interceptor.
- Mostrar toast/error UX.
- Mostrar validaciones de campo.
- No mostrar stack traces.
- Manejar `401` redirigiendo a login.
- Manejar `403` mostrando mensaje de permisos.
- Mostrar errores claros al cambiar password.

---

## 12. Próximos pasos frontend

Prioridades siguientes:

1. MVP hardening.
2. Mejorar empty states.
3. Mejorar loading states.
4. Dashboard analytics.
5. Mejorar tests de servicios/componentes.
6. Preparar producción.
