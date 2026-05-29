# TAMIAS — Diseño de Frontend Angular MVP

Este documento define el diseño técnico del frontend para el MVP de TAMIAS.

Debe usarse como fuente de verdad para implementar:

- Estructura del proyecto Angular.
- Routing.
- Layout.
- Guards.
- Interceptors.
- Servicios por módulo.
- Componentes reutilizables.
- Formularios reactivos.
- Manejo de errores.
- Integración con APIs REST.
- FullCalendar.
- UI del AI Assistant.
- Estándares de código.
- Estrategia de pruebas.

Este documento se basa en:

- `01-architecture-mvp.md`
- `PROJECT_CONTEXT.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `02-database-design-mvp.md`
- `03-api-design-mvp.md`
- `04-backend-design-mvp.md`

---

## 1. Objetivo

El objetivo del frontend de TAMIAS es proporcionar una interfaz web administrativa, clara y eficiente para que propietarios y administradores de alojamientos pequeños puedan controlar su operación diaria.

El frontend debe permitir administrar:

- Autenticación.
- Dashboard.
- Propiedades.
- Catálogos.
- Mantenimientos.
- Mantenimientos programados.
- Calendario.
- Reservaciones.
- Tareas.
- Compras.
- Documentos.
- Asistente IA.

---

## 2. Stack frontend

Stack definido:

```text
Angular
TypeScript
Bootstrap
Angular Reactive Forms
FullCalendar
RxJS
Angular Router
HTTP Client
```

Recomendaciones:

- Usar Angular standalone components.
- Usar TypeScript estricto.
- Usar Reactive Forms para formularios.
- Usar Bootstrap para acelerar UI.
- Usar FullCalendar para calendario de mantenimientos.
- Evitar librerías UI pesadas al inicio para mantener control y simplicidad.

---

## 3. Tipo de arquitectura frontend

El frontend se organizará por features.

Estructura principal:

```text
core
shared
features
```

Principios:

- `core` contiene servicios globales y configuración de aplicación.
- `shared` contiene componentes reutilizables.
- `features` contiene módulos/pantallas del dominio.
- Cada feature debe encapsular sus páginas, componentes, modelos y servicios.
- No duplicar lógica entre features.
- No llamar APIs directamente desde componentes si la lógica se puede mover a services.

---

## 4. Estructura general del proyecto

Estructura recomendada:

```text
frontend/
  src/
    app/
      app.config.ts
      app.routes.ts
      core/
      shared/
      features/
    assets/
    environments/
      environment.ts
      environment.prod.ts
  angular.json
  package.json
  tsconfig.json
```

---

## 5. Estructura detallada

```text
src/app
  core/
    auth/
      auth.service.ts
      auth.models.ts
      token-storage.service.ts
    guards/
      auth.guard.ts
      role.guard.ts
    interceptors/
      auth.interceptor.ts
      error.interceptor.ts
    layout/
      main-layout/
      auth-layout/
    services/
      api.service.ts
      notification.service.ts
      loading.service.ts
    models/
      page-response.model.ts
      error-response.model.ts
      id-name.model.ts

  shared/
    components/
      page-header/
      data-table/
      confirm-dialog/
      loading-spinner/
      empty-state/
      status-badge/
      file-upload/
      form-error/
    pipes/
      enum-label.pipe.ts
      file-size.pipe.ts
    validators/
      date-range.validator.ts
    utils/
      date.utils.ts

  features/
    auth/
      login/
    dashboard/
    properties/
      pages/
      components/
      services/
      models/
    catalogs/
      pages/
      components/
      services/
      models/
    maintenance/
      pages/
      components/
      services/
      models/
    scheduled-maintenance/
      pages/
      components/
      services/
      models/
    reservations/
      pages/
      components/
      services/
      models/
    tasks/
      pages/
      components/
      services/
      models/
    purchases/
      pages/
      components/
      services/
      models/
    documents/
      pages/
      components/
      services/
      models/
    ai-assistant/
      pages/
      components/
      services/
      models/
    users/
      pages/
      components/
      services/
      models/
```

---

# 6. Configuración inicial

## 6.1 Crear proyecto

Comando recomendado:

```bash
ng new frontend --standalone --routing --style=scss --package-manager=npm
```

## 6.2 TypeScript estricto

Mantener `strict: true` en `tsconfig.json`.

Validar:

```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true
  }
}
```

## 6.3 Dependencias iniciales

```bash
npm install bootstrap
npm install @fullcalendar/angular @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/interaction
```

Opcional:

```bash
npm install bootstrap-icons
```

---

# 7. Environments

## 7.1 environment.ts

```ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1'
};
```

## 7.2 environment.prod.ts

```ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://tamias-api.onrender.com/api/v1'
};
```

Nota:

El URL final de backend puede cambiar según Render.

---

# 8. Routing

## 8.1 Rutas principales

Archivo:

```text
src/app/app.routes.ts
```

Rutas recomendadas:

```text
/login

/app
  /dashboard
  /properties
  /properties/new
  /properties/:id
  /properties/:id/edit

  /catalogs
  /catalogs/maintenance-categories
  /catalogs/maintenance-types
  /catalogs/maintenance-people
  /catalogs/platforms
  /catalogs/suppliers
  /catalogs/cities
  /catalogs/materials
  /catalogs/brands
  /catalogs/task-templates

  /maintenance-records
  /maintenance-records/new
  /maintenance-records/:id
  /maintenance-records/:id/edit

  /scheduled-maintenance
  /scheduled-maintenance/calendar
  /scheduled-maintenance/new
  /scheduled-maintenance/:id
  /scheduled-maintenance/:id/edit

  /reservations
  /reservations/new
  /reservations/:id
  /reservations/:id/edit

  /task-lists
  /task-lists/new
  /task-lists/:id
  /task-lists/:id/edit

  /purchase-lists
  /purchase-lists/new
  /purchase-lists/:id
  /purchase-lists/:id/edit

  /documents
  /documents/new
  /documents/:id

  /ai-assistant

  /users
  /users/new
  /users/:id/edit
```

---

## 8.2 Route guards

Rutas bajo `/app` deben usar `authGuard`.

Rutas sensibles deben usar `roleGuard`.

Ejemplo:

```ts
{
  path: 'users',
  canActivate: [authGuard, roleGuard],
  data: { roles: ['ADMINISTRATOR'] },
  loadComponent: () => import('./features/users/pages/user-list/user-list.component')
    .then(m => m.UserListComponent)
}
```

---

# 9. Layout

## 9.1 AuthLayout

Usado para:

```text
/login
```

Debe contener:

- Card centrada.
- Logo/nombre TAMIAS.
- Formulario de login.
- Mensajes de error.

---

## 9.2 MainLayout

Usado para rutas autenticadas.

Debe contener:

- Sidebar.
- Topbar.
- Área principal.
- Nombre de organización.
- Usuario actual.
- Botón de logout.
- Navegación principal.

Estructura visual:

```text
+--------------------------------------------------+
| Topbar: TAMIAS | Organization | User | Logout     |
+----------------------+---------------------------+
| Sidebar              | Page content              |
| - Dashboard          |                           |
| - Properties         |                           |
| - Maintenance        |                           |
| - Reservations       |                           |
| - Purchases          |                           |
| - Documents          |                           |
| - AI Assistant       |                           |
| - Catalogs           |                           |
| - Users              |                           |
+----------------------+---------------------------+
```

---

# 10. Navegación principal

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

Visibilidad por rol:

| Menú | Administrator | Property Manager | Maintenance Staff | Read Only |
|---|---:|---:|---:|---:|
| Dashboard | Yes | Yes | Yes | Yes |
| Properties | Yes | Yes | Yes | Yes |
| Maintenance | Yes | Yes | Yes | Yes |
| Scheduled Maintenance | Yes | Yes | Yes | Yes |
| Reservations | Yes | Yes | Limited | Yes |
| Tasks | Yes | Yes | Yes | Yes |
| Purchase Lists | Yes | Yes | Limited | Yes |
| Documents | Yes | Yes | Limited | Yes |
| AI Assistant | Yes | Yes | Yes | Yes |
| Catalogs | Yes | Yes | Read | Read |
| Users | Yes | No | No | No |

---

# 11. Autenticación frontend

## 11.1 AuthService

Responsabilidades:

- Login.
- Logout.
- Obtener usuario actual.
- Exponer estado de autenticación.
- Validar roles.

Ejemplo:

```ts
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, request)
      .pipe(
        tap(response => {
          this.tokenStorage.setToken(response.accessToken);
          this.currentUserSubject.next(response.user);
        })
      );
  }

  logout(): void {
    this.tokenStorage.clear();
    this.currentUserSubject.next(null);
  }

  hasRole(roles: string[]): boolean {
    const user = this.currentUserSubject.value;
    return !!user && roles.includes(user.role);
  }
}
```

---

## 11.2 TokenStorageService

Responsabilidades:

- Guardar token.
- Leer token.
- Eliminar token.

MVP:

```text
localStorage
```

Nota:

Para mayor seguridad futura, se puede evaluar cookie HttpOnly desde backend, pero para MVP y simplicidad se usará JWT en localStorage con cuidados básicos.

---

## 11.3 AuthInterceptor

Responsabilidades:

- Agregar header Authorization.
- No agregar token a URLs externas.
- Manejar token ausente.

```ts
Authorization: Bearer {token}
```

---

## 11.4 ErrorInterceptor

Responsabilidades:

- Capturar errores HTTP.
- Redirigir a login si `401`.
- Mostrar mensaje si `403`.
- Normalizar errores del backend.
- Evitar duplicar manejo de errores en cada componente.

---

# 12. Modelos transversales

## 12.1 PageResponse

```ts
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
```

## 12.2 ErrorResponse

```ts
export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details: FieldErrorResponse[];
}

export interface FieldErrorResponse {
  field: string;
  message: string;
}
```

## 12.3 IdName

```ts
export interface IdName {
  id: string;
  name: string;
}
```

---

# 13. Servicios HTTP

## 13.1 ApiService base

Crear un servicio base opcional para operaciones comunes.

```ts
@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  get<T>(url: string, params?: Record<string, unknown>): Observable<T> {
    return this.http.get<T>(`${environment.apiBaseUrl}${url}`, { params: this.toHttpParams(params) });
  }

  post<T>(url: string, body: unknown): Observable<T> {
    return this.http.post<T>(`${environment.apiBaseUrl}${url}`, body);
  }

  put<T>(url: string, body: unknown): Observable<T> {
    return this.http.put<T>(`${environment.apiBaseUrl}${url}`, body);
  }

  patch<T>(url: string, body: unknown): Observable<T> {
    return this.http.patch<T>(`${environment.apiBaseUrl}${url}`, body);
  }

  delete<T>(url: string): Observable<T> {
    return this.http.delete<T>(`${environment.apiBaseUrl}${url}`);
  }

  private toHttpParams(params?: Record<string, unknown>): HttpParams {
    let httpParams = new HttpParams();

    if (!params) {
      return httpParams;
    }

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return httpParams;
  }
}
```

---

## 13.2 Servicios por feature

Cada feature debe tener su propio service.

Ejemplos:

```text
PropertyService
MaintenanceCategoryService
MaintenanceRecordService
ScheduledMaintenanceService
ReservationService
TaskListService
PurchaseListService
DocumentService
AiAssistantService
UserService
```

Ejemplo:

```ts
@Injectable({ providedIn: 'root' })
export class PropertyService {
  private readonly baseUrl = '/properties';

  constructor(private api: ApiService) {}

  findAll(filter: PropertyFilter): Observable<PageResponse<PropertySummary>> {
    return this.api.get<PageResponse<PropertySummary>>(this.baseUrl, filter);
  }

  findById(id: string): Observable<PropertyDetail> {
    return this.api.get<PropertyDetail>(`${this.baseUrl}/${id}`);
  }

  create(request: PropertyRequest): Observable<PropertyDetail> {
    return this.api.post<PropertyDetail>(this.baseUrl, request);
  }

  update(id: string, request: PropertyRequest): Observable<PropertyDetail> {
    return this.api.put<PropertyDetail>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

---

# 14. Componentes compartidos

## 14.1 PageHeaderComponent

Uso:

- Título de página.
- Descripción.
- Botón principal.

Ejemplo:

```text
Properties
Manage your rental properties.
[New Property]
```

---

## 14.2 DataTableComponent

Uso:

- Mostrar listados.
- Soportar loading.
- Empty state.
- Acciones por fila.

MVP:

Puede ser un componente simple o implementarse directamente con Bootstrap tables en cada feature.

Recomendación:

- No crear una tabla genérica demasiado compleja al inicio.
- Empezar con tablas por feature y extraer componentes cuando se repita mucho.

---

## 14.3 ConfirmDialogComponent

Uso:

- Confirmar eliminación.
- Confirmar cancelación.
- Confirmar reprogramación.

MVP:

- Puede ser un modal Bootstrap reutilizable.

---

## 14.4 LoadingSpinnerComponent

Uso:

- Cargas globales o por sección.

---

## 14.5 EmptyStateComponent

Uso:

- Mostrar mensaje cuando no hay datos.

Ejemplo:

```text
No properties found.
Create your first property to start using TAMIAS.
```

---

## 14.6 StatusBadgeComponent

Uso:

- Mostrar estados como ACTIVE, COMPLETED, CANCELLED, etc.

---

## 14.7 FileUploadComponent

Uso:

- Subir imágenes de propiedades.
- Subir imágenes de mantenimiento.
- Subir documentos.

Debe validar:

- Extensión.
- Tamaño.
- Tipo de archivo.
- Archivo requerido.

---

## 14.8 FormErrorComponent

Uso:

- Mostrar errores de campos en formularios reactivos.

---

# 15. Formularios reactivos

Todos los formularios deben usar Reactive Forms.

Ejemplo:

```ts
this.form = this.fb.group({
  name: ['', [Validators.required, Validators.maxLength(150)]],
  address: [''],
  description: [''],
  status: ['ACTIVE', Validators.required]
});
```

Reglas:

- Validaciones del frontend deben reflejar validaciones del backend.
- No confiar únicamente en validaciones frontend.
- Mostrar errores por campo.
- Deshabilitar botón submit mientras se envía.
- Mostrar mensaje de éxito o error.

---

## 15.1 Patrón de formulario

Cada formulario debería seguir este flujo:

```text
Init component
  |
If edit mode, load entity
  |
Create form
  |
Load catalogs if needed
  |
User submits
  |
Validate form
  |
Call service
  |
Show success
  |
Navigate back or stay
```

---

# 16. Manejo de errores en UI

## 16.1 Errores de validación

Si backend responde:

```json
{
  "details": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ]
}
```

El frontend debe intentar mapear esos errores al formulario.

---

## 16.2 Errores generales

Mostrar alert Bootstrap o toast:

```text
Could not save property. Please try again.
```

MVP:

- Usar alertas simples.
- Toast service puede agregarse en shared/core.

---

## 16.3 401

Comportamiento:

- Limpiar token.
- Redirigir a `/login`.

---

## 16.4 403

Comportamiento:

- Mostrar mensaje:
  - "You do not have permission to perform this action."

---

## 16.5 404

Comportamiento:

- Mostrar página simple de recurso no encontrado.
- O redirigir al listado con alerta.

---

# 17. Pantallas del MVP

## 17.1 Login

Ruta:

```text
/login
```

Campos:

- Email.
- Password.

Acciones:

- Login.
- Mostrar error de credenciales.

---

## 17.2 Dashboard

Ruta:

```text
/app/dashboard
```

Widgets MVP:

- Total properties.
- Upcoming scheduled maintenance.
- Open task lists.
- Active reservations.
- Recent maintenance records.
- Recent purchase lists.

Nota:

Si backend aún no tiene endpoints agregados, el dashboard puede usar consultas existentes con `size=5`.

---

## 17.3 Properties

Rutas:

```text
/app/properties
/app/properties/new
/app/properties/:id
/app/properties/:id/edit
```

Listado:

- Nombre.
- Dirección.
- Estado.
- Imagen principal.
- Acciones.

Formulario:

- Name.
- Address.
- Description.
- Status.

Detalle:

- Datos principales.
- Imágenes.
- Mantenimientos recientes.
- Reservaciones próximas.
- Documentos relacionados.

MVP:

- Detalle puede iniciar simple.

---

## 17.4 Catalogs

Ruta principal:

```text
/app/catalogs
```

Subrutas:

```text
/app/catalogs/maintenance-categories
/app/catalogs/maintenance-types
/app/catalogs/maintenance-people
/app/catalogs/platforms
/app/catalogs/suppliers
/app/catalogs/cities
/app/catalogs/materials
/app/catalogs/brands
/app/catalogs/task-templates
```

Cada catálogo:

- Listado.
- Crear.
- Editar.
- Eliminar.
- Estado.

MVP:

- Se pueden crear componentes reutilizables para catálogos simples.

---

## 17.5 Maintenance Records

Rutas:

```text
/app/maintenance-records
/app/maintenance-records/new
/app/maintenance-records/:id
/app/maintenance-records/:id/edit
```

Listado:

- Propiedad.
- Categoría.
- Tipo.
- Fecha.
- Costo.
- Estado.

Filtros:

- Propiedad.
- Categoría.
- Tipo.
- Fecha desde/hasta.
- Estado.

Formulario:

- Property.
- Category.
- Type.
- Maintenance Date.
- Description.
- Cost.
- Responsible People.
- Materials Used.
- Images.

Detalle:

- Datos principales.
- Responsables.
- Materiales.
- Imágenes.

---

## 17.6 Scheduled Maintenance

Rutas:

```text
/app/scheduled-maintenance
/app/scheduled-maintenance/calendar
/app/scheduled-maintenance/new
/app/scheduled-maintenance/:id
/app/scheduled-maintenance/:id/edit
```

Listado:

- Propiedad.
- Categoría.
- Tipo.
- Fecha planificada.
- Hora.
- Estado.

Acciones:

- Reprogramar.
- Cancelar.
- Completar.

Calendario:

- FullCalendar.
- Vista mensual.
- Vista semanal opcional.
- Click en evento abre detalle.

Formulario:

- Property.
- Category.
- Type.
- Planned Date.
- Planned Time.
- Description.

Modales:

- Reschedule modal.
- Cancel modal.
- Complete modal.

---

## 17.7 Reservations

Rutas:

```text
/app/reservations
/app/reservations/new
/app/reservations/:id
/app/reservations/:id/edit
```

Listado:

- Propiedad.
- Plataforma.
- Reservation ID.
- Check-in.
- Check-out.
- Huéspedes.
- Valor.
- Estado.

Formulario:

- Property.
- Platform.
- Reservation ID.
- Check In.
- Check Out.
- Supplies Delivered.
- Observations.
- Reservation Value.
- Invoice Number.
- Invoice Series.
- Guests.

Validación:

- Check-out debe ser mayor que check-in.

---

## 17.8 Task Lists

Rutas:

```text
/app/task-lists
/app/task-lists/new
/app/task-lists/:id
/app/task-lists/:id/edit
```

Listado:

- Propiedad.
- Título.
- Fecha creación.
- Fecha vencimiento.
- Progreso.
- Estado.

Formulario:

- Property.
- Reservation optional.
- Maintenance Record optional.
- Title.
- Due Date.
- Items.

Items:

- Task name.
- Responsible person.
- Completed.
- Completion date.

---

## 17.9 Purchase Lists

Rutas:

```text
/app/purchase-lists
/app/purchase-lists/new
/app/purchase-lists/:id
/app/purchase-lists/:id/edit
```

Listado:

- Propiedad.
- Ciudad.
- Proveedor.
- Fecha compra.
- Total estimado.
- Progreso.
- Estado.

Formulario:

- Property optional.
- City.
- Supplier.
- Purchase Date.
- Notes.
- Items.

Items:

- Material.
- Brand.
- Item Name.
- Quantity.
- Unit.
- Estimated Price.
- Purchased.
- Notes.

---

## 17.10 Documents

Rutas:

```text
/app/documents
/app/documents/new
/app/documents/:id
```

Listado:

- Propiedad.
- Tipo.
- Título.
- Archivo.
- Estado de procesamiento.
- Estado.
- Fecha.

Formulario upload:

- Property optional.
- Document Type.
- Title.
- Description.
- File.

Acciones:

- Download.
- Process for AI.
- Delete.

Detalle:

- Metadatos.
- Download URL.
- Processing status.
- Chunks procesados, opcional en fase futura.

---

## 17.11 AI Assistant

Ruta:

```text
/app/ai-assistant
```

UI recomendada:

```text
+--------------------------------------------------+
| Property selector                                |
+--------------------------------------------------+
| Chat messages                                    |
|                                                  |
| User: ¿Qué dice el reglamento sobre mascotas?    |
| AI: Según el documento House Rules...            |
| Sources: House Rules, chunk 3                    |
+--------------------------------------------------+
| Input message                              Send   |
+--------------------------------------------------+
```

Funciones MVP:

- Seleccionar propiedad opcional.
- Escribir pregunta.
- Enviar a `/api/v1/ai/chat`.
- Mostrar respuesta.
- Mostrar fuentes.
- Mostrar confianza.
- Mostrar mensaje cuando no haya evidencia suficiente.

Reglas UI:

- No presentar respuestas sin fuente como verdad documental.
- Diferenciar visualmente answer y sources.
- Mostrar loading mientras responde.
- Manejar error de IA con mensaje claro.

---

## 17.12 Users

Rutas:

```text
/app/users
/app/users/new
/app/users/:id/edit
```

Solo Administrator.

Listado:

- Nombre.
- Email.
- Rol.
- Estado.

Formulario:

- First Name.
- Last Name.
- Email.
- Password temporal, solo create.
- Role.
- Status.

---

# 18. FullCalendar

## 18.1 Uso

Se usará para visualizar mantenimientos programados.

Endpoint:

```http
GET /api/v1/scheduled-maintenance/calendar?from=2026-06-01&to=2026-06-30
```

## 18.2 Evento esperado

```ts
export interface ScheduledMaintenanceCalendarEvent {
  id: string;
  title: string;
  start: string;
  end?: string | null;
  status: string;
  propertyName: string;
}
```

## 18.3 Configuración inicial

```ts
calendarOptions: CalendarOptions = {
  initialView: 'dayGridMonth',
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  events: [],
  eventClick: (info) => this.openDetail(info.event.id)
};
```

## 18.4 Colores

MVP:

- Se puede usar clases CSS según estado.
- No hardcodear demasiada lógica visual en el componente.

---

# 19. Document upload

## 19.1 FileUploadComponent

Debe soportar:

- Drag & drop opcional.
- Input file básico.
- Validación de tamaño.
- Validación de tipo.
- Mostrar nombre del archivo.
- Mostrar tamaño.
- Limpiar selección.

## 19.2 Subida con FormData

Ejemplo:

```ts
const formData = new FormData();
formData.append('propertyId', request.propertyId);
formData.append('documentType', request.documentType);
formData.append('title', request.title);
formData.append('description', request.description ?? '');
formData.append('file', request.file);

return this.http.post<DocumentResponse>(`${environment.apiBaseUrl}/documents`, formData);
```

---

# 20. Estado de carga

MVP:

- Usar loading local por componente.
- Crear `LoadingService` global solo si hace falta.

Patrón simple:

```ts
loading = false;

loadData(): void {
  this.loading = true;

  this.service.findAll(this.filter)
    .pipe(finalize(() => this.loading = false))
    .subscribe(...);
}
```

---

# 21. Manejo de estado

MVP:

- No usar NgRx.
- Usar services + RxJS + estado local de componentes.
- Usar BehaviorSubject para auth/current user.

Razón:

- NgRx agregaría complejidad innecesaria para MVP.
- El dominio todavía está evolucionando.
- El tamaño inicial no lo justifica.

Futuro:

- Evaluar state management si la aplicación crece mucho.

---

# 22. Bootstrap y estilos

## 22.1 Uso de Bootstrap

Usar Bootstrap para:

- Grid.
- Cards.
- Forms.
- Buttons.
- Alerts.
- Badges.
- Modals.
- Tables.
- Navbar/sidebar.

## 22.2 Estructura SCSS

```text
src/
  styles.scss
  styles/
    _variables.scss
    _layout.scss
    _forms.scss
    _tables.scss
    _badges.scss
```

## 22.3 Branding inicial

Nombre:

```text
TAMIAS
```

Estilo recomendado:

- Limpio.
- Administrativo.
- Profesional.
- Colores sobrios.
- Buen contraste.
- Responsive para desktop y tablet.

MVP:

- Priorizar funcionalidad y claridad sobre diseño visual avanzado.

---

# 23. Responsive design

TAMIAS debe ser usable en:

- Desktop.
- Laptop.
- Tablet.
- Mobile básico.

Prioridad MVP:

1. Desktop.
2. Laptop.
3. Tablet.
4. Mobile.

No diseñar como mobile-first estricto en MVP, pero evitar pantallas inutilizables en móvil.

---

# 24. Accesibilidad básica

Aplicar:

- Labels en formularios.
- Botones con texto claro.
- Contraste suficiente.
- Mensajes de error visibles.
- Navegación razonable por teclado.
- Uso correcto de `aria-label` donde aplique.

---

# 25. Validaciones frontend

Las validaciones frontend deben reflejar las del backend.

Ejemplos:

## Property

```text
name required, max 150
status required
```

## Reservation

```text
property required
checkIn required
checkOut required
checkOut > checkIn
reservationValue >= 0
```

## Maintenance

```text
property required
category required
type required
maintenanceDate required
description required
cost >= 0
```

## Purchase Item

```text
itemName required
quantity > 0
estimatedPrice >= 0
```

## Document

```text
documentType required
title required
file required
file type allowed
file size allowed
```

---

# 26. Role-based UI

El frontend debe ocultar acciones según rol, pero esto no reemplaza seguridad backend.

Ejemplos:

- Read Only no ve botones de crear/editar/eliminar.
- Maintenance Staff puede completar tareas, pero no administrar usuarios.
- Users menu solo visible para Administrator.
- Delete buttons solo para roles autorizados.

Crear helper:

```ts
canCreate(module: string): boolean
canEdit(module: string): boolean
canDelete(module: string): boolean
```

MVP:

- Puede resolverse con `AuthService.hasRole([...])`.

---

# 27. Integración con backend

## 27.1 URLs

Todas las llamadas deben usar:

```ts
environment.apiBaseUrl
```

No hardcodear URLs en services.

## 27.2 Fechas

Reglas:

- Usar ISO strings con backend.
- Para fechas sin hora usar `YYYY-MM-DD`.
- Para fecha/hora usar ISO 8601.
- Mostrar fechas en formato amigable en UI.

## 27.3 Montos

- Usar number en TypeScript.
- Mostrar con pipe de currency o formato personalizado.
- No hacer cálculos financieros complejos en frontend como fuente de verdad.

---

# 28. Testing frontend

## 28.1 Tipos de pruebas

MVP:

- Unit tests para services.
- Unit tests para guards.
- Unit tests para interceptors.
- Tests básicos de componentes críticos.

## 28.2 Prioridad

Primera prioridad:

1. AuthService.
2. AuthGuard.
3. RoleGuard.
4. AuthInterceptor.
5. ErrorInterceptor.
6. PropertyService.
7. MaintenanceRecordService.
8. DocumentService.
9. AiAssistantService.

## 28.3 Formularios

Probar validadores importantes:

- Reservation date range.
- Required fields.
- File validation.
- Numeric validations.

---

# 29. Build y calidad

## 29.1 Comandos

```bash
npm install
npm run start
npm run build
npm run test
```

## 29.2 Scripts recomendados

`package.json`:

```json
{
  "scripts": {
    "start": "ng serve",
    "build": "ng build",
    "test": "ng test",
    "lint": "ng lint"
  }
}
```

Nota:

Si Angular CLI no incluye ESLint por defecto, puede configurarse después.

---

# 30. Variables de despliegue

Vercel debe configurar:

```text
API base URL
```

En Angular normalmente se maneja con archivos environment durante build.

Para despliegues más flexibles, en fase futura se puede usar configuración runtime.

---

# 31. Orden recomendado de implementación frontend

Implementar en este orden:

1. Crear proyecto Angular.
2. Configurar Bootstrap.
3. Configurar environments.
4. Crear rutas base.
5. Crear AuthLayout y MainLayout.
6. Crear Login.
7. Crear AuthService.
8. Crear TokenStorageService.
9. Crear AuthInterceptor.
10. Crear AuthGuard.
11. Crear RoleGuard.
12. Crear componentes shared básicos.
13. Crear Dashboard básico.
14. Crear Properties.
15. Crear Catalogs.
16. Crear Maintenance Records.
17. Crear Scheduled Maintenance.
18. Integrar FullCalendar.
19. Crear Reservations.
20. Crear Task Lists.
21. Crear Purchase Lists.
22. Crear Documents.
23. Crear AI Assistant.
24. Crear Users.
25. Mejorar manejo de errores.
26. Mejorar responsive.
27. Agregar tests principales.
28. Preparar build Vercel.

---

# 32. Reglas para no romper el diseño

Antes de implementar cualquier pantalla o servicio, validar:

1. ¿Pertenece al MVP?
2. ¿Consume endpoints definidos en `03-api-design-mvp.md`?
3. ¿Respeta roles?
4. ¿No duplica lógica innecesariamente?
5. ¿Usa DTOs/modelos tipados?
6. ¿Maneja loading?
7. ¿Maneja errores?
8. ¿Tiene validaciones frontend?
9. ¿No expone secretos?
10. ¿No hardcodea URLs de backend?

---

# 33. Decisiones abiertas

## 33.1 Librería UI

MVP:

- Bootstrap.

Futuro:

- Evaluar Angular Material, PrimeNG u otra librería si se necesita mayor velocidad o componentes avanzados.

## 33.2 Toast notifications

MVP:

- Alerts simples.

Futuro:

- Toast service reutilizable.

## 33.3 State management

MVP:

- Services + RxJS.

Futuro:

- Evaluar NgRx, Signal Store o estado más avanzado si la complejidad aumenta.

## 33.4 Runtime config

MVP:

- Angular environments.

Futuro:

- Runtime config para cambiar API URL sin rebuild.

---

# 34. Próximo entregable recomendado

Después de este documento, el siguiente entregable recomendado es:

```text
TAMIAS — Diseño de IA MVP
```

Archivo sugerido:

```text
docs/06-ai-design-mvp.md
```

Ese documento debe definir:

- Alcance IA del MVP.
- RAG sobre documentos.
- Procesamiento de documentos.
- Extracción de texto.
- Chunking.
- Embeddings.
- Chroma.
- Prompt base.
- Respuestas con fuentes.
- Manejo de incertidumbre.
- Seguridad multi-tenant en IA.
- Diseño futuro de tool calling.
- Diseño futuro de blueprint analysis.
