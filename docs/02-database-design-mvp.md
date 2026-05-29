# TAMIAS — Diseño de Base de Datos MVP

Este documento define el diseño inicial de base de datos para el MVP de TAMIAS.

Debe usarse como fuente de verdad para:

- Entidades JPA.
- Migraciones Flyway.
- Relaciones entre módulos.
- APIs REST.
- Formularios del frontend.
- Seguridad multi-tenant.
- Futuras consultas de IA mediante tool calling.
- Reportes y analítica.

TAMIAS usará PostgreSQL como base de datos principal.

---

## 1. Objetivo del diseño

El objetivo de este diseño es crear una base de datos sólida, mantenible y escalable para el MVP de TAMIAS.

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
- Purchase Lists
- Documents
- AI Document Search con RAG
- Basic Deploy

Quedan fuera del MVP inicial:

- Billing/subscriptions.
- Integraciones directas con Airbnb, Booking o VRBO.
- JasperReports avanzados.
- Blueprint Analysis.
- AI Agents especializados.
- Tool Calling completo.
- Inventario formal.
- Notificaciones automáticas avanzadas.

---

## 2. Principios generales

La base de datos debe seguir estos principios:

1. Diseño multi-tenant desde el inicio.
2. Uso de UUID como identificador principal.
3. Auditoría básica en tablas principales.
4. Soft delete en entidades operativas importantes.
5. Integridad referencial mediante foreign keys.
6. Constraints únicos considerando `organization_id`.
7. Índices para consultas frecuentes.
8. Evitar duplicación innecesaria.
9. Evitar lógica crítica solo en frontend.
10. Preparar el modelo para futuras consultas con IA.

---

## 3. Reglas obligatorias de diseño

Estas reglas aplican al diseño de base de datos del MVP y deben mantenerse durante la implementación.

### 3.1 IDs principales con UUID

Todas las entidades principales deben usar UUID como clave primaria.

Ejemplo:

```sql
id UUID PRIMARY KEY
```

Ventajas:

- Evita exponer secuencias incrementales.
- Es adecuado para sistemas SaaS.
- Facilita integraciones futuras.
- Reduce riesgo de enumeración de registros.

Tablas que deben usar UUID:

- organizations
- users
- properties
- maintenance_records
- scheduled_maintenance
- reservations
- purchase_lists
- documents
- y demás entidades principales.

---

### 3.2 `organization_id` en entidades operativas

Toda entidad que pertenezca a una organización debe incluir `organization_id`.

Ejemplo:

```sql
organization_id UUID NOT NULL REFERENCES organizations(id)
```

Entidades que deben tener `organization_id`:

- users, mediante user_organizations
- properties
- catalogs
- maintenance records
- scheduled maintenance
- reservations
- purchase lists
- documents
- task lists
- guests
- suppliers
- platforms
- materials
- cities
- brands

Regla crítica:

> El backend siempre debe filtrar por la organización del usuario autenticado.

No se debe confiar en un `organization_id` recibido libremente desde el frontend.

---

### 3.3 `created_at` y `updated_at` en tablas principales

Las tablas principales deben incluir:

```sql
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
```

Esto permite:

- Auditoría básica.
- Ordenamiento cronológico.
- Trazabilidad.
- Depuración de datos.
- Reportes futuros.

---

### 3.4 `created_by` y `updated_by` donde tenga valor operativo

Cuando sea útil saber qué usuario creó o modificó un registro, agregar:

```sql
created_by UUID REFERENCES users(id),
updated_by UUID REFERENCES users(id)
```

Debe usarse especialmente en:

- properties
- maintenance_records
- scheduled_maintenance
- reservations
- purchase_lists
- documents
- task_lists
- catalog items importantes

No es obligatorio en tablas puramente relacionales o de bajo valor operativo, como tablas puente simples.

---

### 3.5 `status` en entidades que no conviene borrar físicamente

Las entidades importantes deben tener `status`.

Ejemplo:

```sql
status VARCHAR(30) NOT NULL
```

Se recomienda usar `status` en:

- organizations
- users
- properties
- maintenance_records
- scheduled_maintenance
- reservations
- purchase_lists
- documents
- catalog items

Ejemplos de estados:

```text
ACTIVE
INACTIVE
DELETED
```

Para entidades específicas:

```text
SCHEDULED
COMPLETED
RESCHEDULED
CANCELLED
```

---

### 3.6 Soft delete para datos importantes

Los datos operativos importantes no deberían eliminarse físicamente de forma inmediata.

Agregar:

```sql
deleted_at TIMESTAMP WITH TIME ZONE NULL,
deleted_by UUID REFERENCES users(id)
```

Aplicar soft delete en:

- organizations
- users
- properties
- maintenance_records
- scheduled_maintenance
- reservations
- purchase_lists
- documents
- catalog items

Ventajas:

- Recuperación de datos.
- Auditoría.
- Evitar pérdida accidental.
- Mantener historial para reportes.

---

### 3.7 Constraints únicos considerando `organization_id`

Los nombres de catálogos y entidades configurables deben ser únicos por organización, no globalmente.

Ejemplo:

```sql
UNIQUE (organization_id, name)
```

Aplicar a:

- maintenance_categories
- maintenance_types
- maintenance_people
- platforms
- suppliers
- cities
- materials
- supplies
- brands
- task_templates

Esto permite que dos organizaciones diferentes tengan catálogos con el mismo nombre sin conflicto.

---

## 4. Convenciones de nombres

### Tablas

Usar nombres en plural, snake_case:

```text
organizations
users
properties
maintenance_records
scheduled_maintenance
purchase_lists
purchase_items
```

### Columnas

Usar snake_case:

```text
created_at
updated_at
organization_id
property_id
maintenance_date
```

### Foreign keys

Usar el patrón:

```text
{entity}_id
```

Ejemplos:

```text
organization_id
property_id
reservation_id
document_id
```

### Constraints

Usar nombres explícitos:

```text
pk_properties
fk_properties_organization
uk_maintenance_categories_organization_name
idx_properties_organization_status
```

---

## 5. Tipos de datos recomendados

| Caso | Tipo PostgreSQL recomendado |
|---|---|
| ID principal | UUID |
| Texto corto | VARCHAR(n) |
| Texto largo | TEXT |
| Fechas con hora | TIMESTAMP WITH TIME ZONE |
| Fecha sin hora | DATE |
| Hora sin fecha | TIME |
| Montos | NUMERIC(12,2) |
| Cantidades | NUMERIC(12,2) |
| Booleanos | BOOLEAN |
| Estados | VARCHAR(30) |
| Emails | VARCHAR(150) |
| Teléfonos | VARCHAR(50) |
| URLs o S3 keys | TEXT |

---

## 6. Modelo lógico general

```text
Organization
  ├── UserOrganization
  │     └── User
  ├── Properties
  │     ├── PropertyImages
  │     ├── MaintenanceRecords
  │     ├── ScheduledMaintenance
  │     ├── Reservations
  │     ├── PurchaseLists
  │     └── Documents
  ├── Catalogs
  │     ├── MaintenanceCategories
  │     ├── MaintenanceTypes
  │     ├── MaintenancePeople
  │     ├── Platforms
  │     ├── Suppliers
  │     ├── Cities
  │     ├── Materials
  │     ├── Supplies
  │     ├── Brands
  │     └── TaskTemplates
  └── AI
        ├── Document metadata
        └── Chroma vector records
```

---

# 7. Tablas del MVP

## 7.1 organizations

Representa una organización, propietario, empresa o administrador que usa TAMIAS.

```sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);
```

Constraints recomendados:

```sql
ALTER TABLE organizations
ADD CONSTRAINT uk_organizations_name UNIQUE (name);
```

Índices:

```sql
CREATE INDEX idx_organizations_status ON organizations(status);
```

---

## 7.2 users

Representa usuarios del sistema.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);
```

Constraints:

```sql
ALTER TABLE users
ADD CONSTRAINT uk_users_email UNIQUE (email);
```

Índices:

```sql
CREATE INDEX idx_users_status ON users(status);
```

Notas:

- El password nunca debe guardarse en texto plano.
- Usar BCrypt mediante Spring Security.

---

## 7.3 roles

Representa roles base del sistema.

Roles iniciales:

- ADMINISTRATOR
- PROPERTY_MANAGER
- MAINTENANCE_STAFF
- READ_ONLY

```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Constraints:

```sql
ALTER TABLE roles
ADD CONSTRAINT uk_roles_code UNIQUE (code);
```

---

## 7.4 user_organizations

Relaciona usuarios con organizaciones y roles.

Permite que un usuario pueda pertenecer a más de una organización en el futuro.

```sql
CREATE TABLE user_organizations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Constraints:

```sql
ALTER TABLE user_organizations
ADD CONSTRAINT uk_user_organizations_user_org UNIQUE (user_id, organization_id);
```

Índices:

```sql
CREATE INDEX idx_user_organizations_user ON user_organizations(user_id);
CREATE INDEX idx_user_organizations_organization ON user_organizations(organization_id);
CREATE INDEX idx_user_organizations_role ON user_organizations(role_id);
```

---

# 8. Propiedades

## 8.1 properties

Representa casas, apartamentos, bungalows, cabañas o villas.

```sql
CREATE TABLE properties (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    address TEXT,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE properties
ADD CONSTRAINT uk_properties_organization_name UNIQUE (organization_id, name);
```

Índices:

```sql
CREATE INDEX idx_properties_organization ON properties(organization_id);
CREATE INDEX idx_properties_organization_status ON properties(organization_id, status);
```

---

## 8.2 property_images

Imágenes asociadas a una propiedad.

```sql
CREATE TABLE property_images (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Índices:

```sql
CREATE INDEX idx_property_images_property ON property_images(property_id);
CREATE INDEX idx_property_images_organization ON property_images(organization_id);
```

---

# 9. Catálogos

Los catálogos serán administrables por organización.

Regla general para catálogos:

```sql
organization_id UUID NOT NULL
name VARCHAR(150) NOT NULL
status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
UNIQUE (organization_id, name)
```

---

## 9.1 maintenance_categories

Ejemplos:

- Well
- Water Filters
- Exterior Walls
- Interior Walls
- Gate
- Gutters
- Cistern
- Pumps
- Doors
- Showers
- House

```sql
CREATE TABLE maintenance_categories (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE maintenance_categories
ADD CONSTRAINT uk_maintenance_categories_org_name UNIQUE (organization_id, name);
```

---

## 9.2 maintenance_types

Ejemplos:

- Repair
- Cleaning
- Painting
- Inspection
- Replacement

```sql
CREATE TABLE maintenance_types (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE maintenance_types
ADD CONSTRAINT uk_maintenance_types_org_name UNIQUE (organization_id, name);
```

---

## 9.3 maintenance_people

Personas responsables de mantenimiento.

```sql
CREATE TABLE maintenance_people (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(150),
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE maintenance_people
ADD CONSTRAINT uk_maintenance_people_org_name UNIQUE (organization_id, full_name);
```

---

## 9.4 platforms

Plataformas de reservación.

Ejemplos:

- Airbnb
- Booking
- VRBO

```sql
CREATE TABLE platforms (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE platforms
ADD CONSTRAINT uk_platforms_org_name UNIQUE (organization_id, name);
```

---

## 9.5 suppliers

Proveedores.

Ejemplos:

- EPA
- Cemaco
- Walmart
- Novex
- PriceSmart

```sql
CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(150),
    website TEXT,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE suppliers
ADD CONSTRAINT uk_suppliers_org_name UNIQUE (organization_id, name);
```

---

## 9.6 cities

Ciudades para compras.

Ejemplos:

- Guatemala
- Quetzaltenango
- Mazatenango

```sql
CREATE TABLE cities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    country VARCHAR(100) DEFAULT 'Guatemala',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE cities
ADD CONSTRAINT uk_cities_org_name UNIQUE (organization_id, name);
```

---

## 9.7 materials

Materiales o suministros utilizados en mantenimientos y compras.

```sql
CREATE TABLE materials (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    unit VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE materials
ADD CONSTRAINT uk_materials_org_name UNIQUE (organization_id, name);
```

---

## 9.8 brands

Marcas de materiales o suministros.

```sql
CREATE TABLE brands (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE brands
ADD CONSTRAINT uk_brands_org_name UNIQUE (organization_id, name);
```

---

## 9.9 task_templates

Catálogo editable de tareas frecuentes.

Ejemplos:

- Clean bathroom
- Check water filters
- Verify towels
- Inspect doors
- Refill supplies

```sql
CREATE TABLE task_templates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE task_templates
ADD CONSTRAINT uk_task_templates_org_name UNIQUE (organization_id, name);
```

---

# 10. Mantenimiento

## 10.1 maintenance_records

Representa un mantenimiento realizado.

```sql
CREATE TABLE maintenance_records (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    category_id UUID NOT NULL REFERENCES maintenance_categories(id),
    type_id UUID NOT NULL REFERENCES maintenance_types(id),
    maintenance_date TIMESTAMP WITH TIME ZONE NOT NULL,
    description TEXT NOT NULL,
    cost NUMERIC(12,2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Índices:

```sql
CREATE INDEX idx_maintenance_records_org_property ON maintenance_records(organization_id, property_id);
CREATE INDEX idx_maintenance_records_org_date ON maintenance_records(organization_id, maintenance_date);
CREATE INDEX idx_maintenance_records_category ON maintenance_records(category_id);
CREATE INDEX idx_maintenance_records_type ON maintenance_records(type_id);
```

Notas:

- `organization_id` también está presente aunque `property_id` ya apunta a una propiedad con organización.
- Esto facilita filtros, seguridad e índices.
- El backend debe validar que la propiedad, categoría y tipo pertenezcan a la misma organización.

---

## 10.2 maintenance_record_people

Relación entre mantenimientos y responsables.

```sql
CREATE TABLE maintenance_record_people (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    maintenance_record_id UUID NOT NULL REFERENCES maintenance_records(id),
    maintenance_person_id UUID NOT NULL REFERENCES maintenance_people(id)
);
```

Constraints:

```sql
ALTER TABLE maintenance_record_people
ADD CONSTRAINT uk_maintenance_record_people UNIQUE (maintenance_record_id, maintenance_person_id);
```

Índices:

```sql
CREATE INDEX idx_maintenance_record_people_record ON maintenance_record_people(maintenance_record_id);
CREATE INDEX idx_maintenance_record_people_person ON maintenance_record_people(maintenance_person_id);
```

---

## 10.3 maintenance_materials_used

Materiales utilizados en un mantenimiento.

```sql
CREATE TABLE maintenance_materials_used (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    maintenance_record_id UUID NOT NULL REFERENCES maintenance_records(id),
    material_id UUID REFERENCES materials(id),
    material_name_snapshot VARCHAR(150) NOT NULL,
    quantity NUMERIC(12,2),
    unit VARCHAR(50),
    notes TEXT
);
```

Notas:

- `material_id` puede ser null si el usuario registra un material libre.
- `material_name_snapshot` conserva el nombre usado aunque el catálogo cambie después.

Índices:

```sql
CREATE INDEX idx_maintenance_materials_record ON maintenance_materials_used(maintenance_record_id);
CREATE INDEX idx_maintenance_materials_material ON maintenance_materials_used(material_id);
```

---

## 10.4 maintenance_record_images

Imágenes o evidencias del mantenimiento.

```sql
CREATE TABLE maintenance_record_images (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    maintenance_record_id UUID NOT NULL REFERENCES maintenance_records(id),
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Índices:

```sql
CREATE INDEX idx_maintenance_record_images_record ON maintenance_record_images(maintenance_record_id);
```

---

## 10.5 scheduled_maintenance

Representa mantenimientos planificados.

```sql
CREATE TABLE scheduled_maintenance (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    category_id UUID NOT NULL REFERENCES maintenance_categories(id),
    type_id UUID NOT NULL REFERENCES maintenance_types(id),
    planned_date DATE NOT NULL,
    planned_time TIME NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    cancellation_reason TEXT,
    reschedule_reason TEXT,
    completed_maintenance_record_id UUID REFERENCES maintenance_records(id),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Estados permitidos:

```text
SCHEDULED
COMPLETED
RESCHEDULED
CANCELLED
```

Índices:

```sql
CREATE INDEX idx_scheduled_maintenance_org_property ON scheduled_maintenance(organization_id, property_id);
CREATE INDEX idx_scheduled_maintenance_org_date ON scheduled_maintenance(organization_id, planned_date);
CREATE INDEX idx_scheduled_maintenance_status ON scheduled_maintenance(status);
```

---

## 10.6 scheduled_maintenance_history

Historial de cambios de un mantenimiento programado.

```sql
CREATE TABLE scheduled_maintenance_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    scheduled_maintenance_id UUID NOT NULL REFERENCES scheduled_maintenance(id),
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    previous_planned_date DATE,
    new_planned_date DATE,
    previous_planned_time TIME,
    new_planned_time TIME,
    reason TEXT,
    changed_by UUID REFERENCES users(id),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Índices:

```sql
CREATE INDEX idx_scheduled_maintenance_history_schedule ON scheduled_maintenance_history(scheduled_maintenance_id);
CREATE INDEX idx_scheduled_maintenance_history_org ON scheduled_maintenance_history(organization_id);
```

---

# 11. Reservaciones

## 11.1 guests

Catálogo de huéspedes/clientes.

```sql
CREATE TABLE guests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Índices:

```sql
CREATE INDEX idx_guests_organization ON guests(organization_id);
CREATE INDEX idx_guests_org_name ON guests(organization_id, full_name);
```

---

## 11.2 reservations

Reservaciones de una propiedad.

```sql
CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    platform_id UUID REFERENCES platforms(id),
    reservation_code VARCHAR(150),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    supplies_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    observations TEXT,
    reservation_value NUMERIC(12,2),
    invoice_number VARCHAR(100),
    invoice_series VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Constraints:

```sql
ALTER TABLE reservations
ADD CONSTRAINT chk_reservations_dates CHECK (check_out > check_in);
```

Índices:

```sql
CREATE INDEX idx_reservations_org_property ON reservations(organization_id, property_id);
CREATE INDEX idx_reservations_org_dates ON reservations(organization_id, check_in, check_out);
CREATE INDEX idx_reservations_platform ON reservations(platform_id);
```

Nota:

- `reservation_code` corresponde al ID de reservación de la plataforma.

---

## 11.3 reservation_guests

Relación entre reservaciones y huéspedes.

```sql
CREATE TABLE reservation_guests (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    guest_id UUID NOT NULL REFERENCES guests(id),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);
```

Constraints:

```sql
ALTER TABLE reservation_guests
ADD CONSTRAINT uk_reservation_guests UNIQUE (reservation_id, guest_id);
```

Índices:

```sql
CREATE INDEX idx_reservation_guests_reservation ON reservation_guests(reservation_id);
CREATE INDEX idx_reservation_guests_guest ON reservation_guests(guest_id);
```

---

# 12. Tareas

Aunque el módulo de tareas puede crecer en fases posteriores, se incluye una versión básica porque forma parte del flujo operativo de TAMIAS.

## 12.1 task_lists

Lista de tareas asociada opcionalmente a una reservación o mantenimiento.

```sql
CREATE TABLE task_lists (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    reservation_id UUID REFERENCES reservations(id),
    maintenance_record_id UUID REFERENCES maintenance_records(id),
    title VARCHAR(150) NOT NULL,
    creation_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Estados:

```text
OPEN
IN_PROGRESS
COMPLETED
CANCELLED
```

Índices:

```sql
CREATE INDEX idx_task_lists_org_property ON task_lists(organization_id, property_id);
CREATE INDEX idx_task_lists_due_date ON task_lists(organization_id, due_date);
CREATE INDEX idx_task_lists_reservation ON task_lists(reservation_id);
CREATE INDEX idx_task_lists_maintenance_record ON task_lists(maintenance_record_id);
```

---

## 12.2 task_items

Items de una lista de tareas.

```sql
CREATE TABLE task_items (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    task_list_id UUID NOT NULL REFERENCES task_lists(id),
    task_template_id UUID REFERENCES task_templates(id),
    task_name VARCHAR(150) NOT NULL,
    responsible_person VARCHAR(150),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completion_date TIMESTAMP WITH TIME ZONE NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Índices:

```sql
CREATE INDEX idx_task_items_task_list ON task_items(task_list_id);
CREATE INDEX idx_task_items_completed ON task_items(organization_id, completed);
```

Notas:

- `task_name` funciona como snapshot del catálogo.
- Si cambia el nombre en `task_templates`, la tarea histórica mantiene el texto original.

---

# 13. Compras

## 13.1 purchase_lists

Representa una lista de compras.

```sql
CREATE TABLE purchase_lists (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID REFERENCES properties(id),
    city_id UUID REFERENCES cities(id),
    supplier_id UUID REFERENCES suppliers(id),
    purchase_date DATE NOT NULL,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Estados:

```text
OPEN
PARTIALLY_PURCHASED
COMPLETED
CANCELLED
```

Índices:

```sql
CREATE INDEX idx_purchase_lists_org_date ON purchase_lists(organization_id, purchase_date);
CREATE INDEX idx_purchase_lists_supplier ON purchase_lists(supplier_id);
CREATE INDEX idx_purchase_lists_city ON purchase_lists(city_id);
CREATE INDEX idx_purchase_lists_property ON purchase_lists(property_id);
```

---

## 13.2 purchase_items

Items de una lista de compra.

```sql
CREATE TABLE purchase_items (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    purchase_list_id UUID NOT NULL REFERENCES purchase_lists(id),
    material_id UUID REFERENCES materials(id),
    brand_id UUID REFERENCES brands(id),
    item_name_snapshot VARCHAR(150) NOT NULL,
    quantity NUMERIC(12,2) NOT NULL DEFAULT 1,
    unit VARCHAR(50),
    estimated_price NUMERIC(12,2),
    purchased BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Índices:

```sql
CREATE INDEX idx_purchase_items_list ON purchase_items(purchase_list_id);
CREATE INDEX idx_purchase_items_material ON purchase_items(material_id);
CREATE INDEX idx_purchase_items_org_purchased ON purchase_items(organization_id, purchased);
```

Notas:

- `item_name_snapshot` permite conservar el nombre usado en la compra aunque el catálogo cambie.
- Esta tabla será importante para futuras preguntas de IA como:
  - ¿Cuándo compré por última vez filtros de agua?
  - ¿Cuánto gasté en materiales este año?

---

# 14. Documentos

## 14.1 documents

Representa documentos importantes asociados a una propiedad.

Tipos esperados:

- HOUSE_RULES
- BATHROOM_RULES
- PROPERTY_SIGNS
- BLUEPRINT
- ELECTRICAL_PLAN
- PLUMBING_PLAN
- DRAINAGE_PLAN
- MANUAL
- OTHER

```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID REFERENCES properties(id),
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID REFERENCES users(id)
);
```

Estados de procesamiento:

```text
PENDING
PROCESSING
PROCESSED
FAILED
```

Índices:

```sql
CREATE INDEX idx_documents_org_property ON documents(organization_id, property_id);
CREATE INDEX idx_documents_org_type ON documents(organization_id, document_type);
CREATE INDEX idx_documents_processing_status ON documents(processing_status);
```

---

## 14.2 document_chunks

Metadatos de chunks procesados para RAG.

Importante:

- El vector embedding se guardará en Chroma.
- PostgreSQL mantendrá metadatos y trazabilidad.

```sql
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    document_id UUID NOT NULL REFERENCES documents(id),
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    vector_store_collection VARCHAR(150),
    vector_store_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Constraints:

```sql
ALTER TABLE document_chunks
ADD CONSTRAINT uk_document_chunks_document_index UNIQUE (document_id, chunk_index);
```

Índices:

```sql
CREATE INDEX idx_document_chunks_document ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_org ON document_chunks(organization_id);
CREATE INDEX idx_document_chunks_vector_store_id ON document_chunks(vector_store_id);
```

Notas:

- `content` puede guardarse en PostgreSQL para trazabilidad y debugging.
- Si en el futuro se desea reducir almacenamiento, puede guardarse solo una referencia al chunk.

---

# 15. IA y trazabilidad de consultas

Para el MVP, se puede agregar una tabla simple para registrar conversaciones o consultas de IA.

## 15.1 ai_chat_sessions

```sql
CREATE TABLE ai_chat_sessions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    property_id UUID REFERENCES properties(id),
    title VARCHAR(150),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Índices:

```sql
CREATE INDEX idx_ai_chat_sessions_org ON ai_chat_sessions(organization_id);
CREATE INDEX idx_ai_chat_sessions_property ON ai_chat_sessions(property_id);
```

---

## 15.2 ai_chat_messages

```sql
CREATE TABLE ai_chat_messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    chat_session_id UUID NOT NULL REFERENCES ai_chat_sessions(id),
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

Roles esperados:

```text
USER
ASSISTANT
SYSTEM
TOOL
```

Índices:

```sql
CREATE INDEX idx_ai_chat_messages_session ON ai_chat_messages(chat_session_id);
CREATE INDEX idx_ai_chat_messages_org ON ai_chat_messages(organization_id);
```

Notas:

- En MVP puede omitirse si se quiere simplificar.
- Es útil para trazabilidad, debugging y mejora del asistente.

---

# 16. Estados y enumeraciones recomendadas

## 16.1 CommonStatus

```text
ACTIVE
INACTIVE
DELETED
```

## 16.2 UserStatus

```text
ACTIVE
INACTIVE
INVITED
LOCKED
DELETED
```

## 16.3 ScheduledMaintenanceStatus

```text
SCHEDULED
COMPLETED
RESCHEDULED
CANCELLED
```

## 16.4 MaintenanceRecordStatus

```text
COMPLETED
CANCELLED
DELETED
```

## 16.5 ReservationStatus

```text
ACTIVE
CANCELLED
COMPLETED
DELETED
```

## 16.6 PurchaseListStatus

```text
OPEN
PARTIALLY_PURCHASED
COMPLETED
CANCELLED
DELETED
```

## 16.7 TaskListStatus

```text
OPEN
IN_PROGRESS
COMPLETED
CANCELLED
DELETED
```

## 16.8 DocumentProcessingStatus

```text
PENDING
PROCESSING
PROCESSED
FAILED
```

## 16.9 DocumentType

```text
HOUSE_RULES
BATHROOM_RULES
PROPERTY_SIGNS
BLUEPRINT
ELECTRICAL_PLAN
PLUMBING_PLAN
DRAINAGE_PLAN
MANUAL
OTHER
```

---

# 17. Índices transversales recomendados

Para mantener buen rendimiento, crear índices en:

## Multi-tenant

```sql
organization_id
```

En todas las tablas operativas.

## Relaciones frecuentes

```sql
property_id
reservation_id
maintenance_record_id
purchase_list_id
document_id
```

## Búsquedas por fecha

```sql
maintenance_date
planned_date
check_in
check_out
purchase_date
due_date
created_at
```

## Estados

```sql
status
processing_status
```

En combinación con `organization_id` cuando sea posible.

Ejemplo:

```sql
CREATE INDEX idx_documents_org_status ON documents(organization_id, status);
```

---

# 18. Constraints importantes

## Fechas de reservación

```sql
CHECK (check_out > check_in)
```

## Nombres únicos por organización

```sql
UNIQUE (organization_id, name)
```

Aplicar a catálogos.

## Relación única usuario-organización

```sql
UNIQUE (user_id, organization_id)
```

## Chunks únicos por documento

```sql
UNIQUE (document_id, chunk_index)
```

---

# 19. Validaciones que debe hacer el backend

La base de datos no debe cargar toda la lógica de negocio. El backend debe validar:

1. Que el usuario pertenezca a la organización.
2. Que la propiedad pertenezca a la organización.
3. Que los catálogos usados pertenezcan a la organización.
4. Que los documentos consultados pertenezcan a la organización.
5. Que un usuario no modifique datos fuera de su organización.
6. Que `check_out` sea mayor que `check_in`.
7. Que un mantenimiento cancelado tenga razón de cancelación.
8. Que un mantenimiento reprogramado tenga razón de reprogramación.
9. Que los archivos tengan tipo permitido.
10. Que los tamaños de archivo estén dentro del límite.
11. Que las consultas de IA respeten permisos.

---

# 20. Consideraciones para Spring Data JPA

## Entidad base recomendada

Crear una clase base para auditoría:

```java
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
```

Para entidades multi-tenant:

```java
@MappedSuperclass
public abstract class TenantEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
```

Para soft delete:

```java
private OffsetDateTime deletedAt;
```

Notas:

- Se puede usar `@PrePersist` y `@PreUpdate`.
- No depender únicamente de Hibernate para seguridad multi-tenant.
- Los servicios deben validar la organización explícitamente.

---

# 21. Migraciones Flyway sugeridas

Orden recomendado de migraciones:

```text
V1__create_organizations.sql
V2__create_users_roles_user_organizations.sql
V3__create_properties.sql
V4__create_catalogs.sql
V5__create_maintenance.sql
V6__create_reservations.sql
V7__create_tasks.sql
V8__create_purchases.sql
V9__create_documents.sql
V10__create_ai_chat_tables.sql
V11__seed_initial_roles.sql
V12__seed_default_catalogs.sql
```

Notas:

- Los seeds deben ser mínimos.
- Los catálogos por defecto pueden crearse para la organización inicial durante el onboarding.
- Los roles base sí pueden ser globales.

---

# 22. Datos semilla recomendados

## Roles

```text
ADMINISTRATOR
PROPERTY_MANAGER
MAINTENANCE_STAFF
READ_ONLY
```

## Maintenance Categories

```text
Well
Water Filters
Exterior Walls
Interior Walls
Gate
Gutters
Cistern
Pumps
Doors
Showers
House
```

## Maintenance Types

```text
Repair
Cleaning
Painting
Inspection
Replacement
```

## Platforms

```text
Airbnb
Booking
VRBO
```

## Cities

```text
Guatemala
Quetzaltenango
Mazatenango
```

## Suppliers

```text
EPA
Cemaco
Walmart
Novex
PriceSmart
```

## Document Types

```text
House Rules
Bathroom Rules
Property Signs
Blueprints
Electrical Plans
Plumbing Plans
Drainage Plans
Manuals
```

Importante:

Los catálogos de negocio deben ser por organización. Durante el onboarding, se pueden crear valores iniciales para cada organización.

---

# 23. Decisiones abiertas

Estas decisiones pueden definirse más adelante:

## 23.1 ¿Permitir múltiples organizaciones por usuario desde el MVP?

La estructura `user_organizations` lo permite.

Recomendación:

- Diseñar la base para soportarlo.
- En UI del MVP, permitir solo una organización activa.

## 23.2 ¿Guardar historial de cambios para todas las entidades?

Recomendación:

- En MVP, historial explícito solo para `scheduled_maintenance`.
- Para el resto, usar auditoría básica.

## 23.3 ¿Usar enums de PostgreSQL o VARCHAR?

Recomendación:

- Usar `VARCHAR(30)` en MVP.
- Validar enums desde Java.
- Evita migraciones complejas cuando cambien estados.

## 23.4 ¿Separar materials y supplies?

Recomendación MVP:

- Usar `materials` como tabla general.
- Si en el futuro se necesita diferenciar, crear categorías o tipo de material.

---

# 24. Qué queda fuera del diseño MVP

No modelar todavía:

- Billing.
- Planes de suscripción.
- Pagos.
- Integraciones externas con plataformas.
- Inventario formal con existencias.
- Movimientos de inventario.
- Reportes PDF avanzados.
- Auditoría avanzada por campo.
- Permisos granulares por acción.
- Workflows automáticos complejos.
- Agentes IA especializados.
- Análisis avanzado de planos.

---

# 25. Resumen de tablas MVP

Tablas principales:

```text
organizations
users
roles
user_organizations

properties
property_images

maintenance_categories
maintenance_types
maintenance_people
platforms
suppliers
cities
materials
brands
task_templates

maintenance_records
maintenance_record_people
maintenance_materials_used
maintenance_record_images
scheduled_maintenance
scheduled_maintenance_history

guests
reservations
reservation_guests

task_lists
task_items

purchase_lists
purchase_items

documents
document_chunks

ai_chat_sessions
ai_chat_messages
```

---

# 26. Próximo entregable recomendado

El siguiente entregable después de este documento debería ser:

```text
TAMIAS — Diseño de APIs REST MVP
```

Archivo sugerido:

```text
docs/03-api-design-mvp.md
```

Este documento debe definir:

- Endpoints.
- Métodos HTTP.
- Request DTOs.
- Response DTOs.
- Validaciones.
- Códigos de respuesta.
- Seguridad por endpoint.
- Reglas multi-tenant.
- Paginación y filtros.
