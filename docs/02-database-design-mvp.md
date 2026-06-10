# TAMIAS — Diseño de Base de Datos MVP

Este documento define el diseño de base de datos para el MVP actual de TAMIAS.

Debe usarse como fuente de verdad para:

- Entidades JPA.
- Migraciones Flyway.
- Relaciones entre módulos.
- APIs REST.
- Formularios del frontend.
- Seguridad multi-tenant.
- Futuras consultas de IA mediante tool calling.
- Reportes y analítica.

TAMIAS usa PostgreSQL como base de datos principal.

---

## 1. Principios generales

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

## 2. Multi-tenancy

Toda entidad que pertenezca a una organización debe incluir:

```sql
organization_id UUID NOT NULL REFERENCES organizations(id)
```

Entidades operativas con `organization_id`:

- users, mediante user_organizations.
- properties.
- property_images.
- catalogs.
- inventory_items.
- maintenance_records.
- maintenance_record_items.
- maintenance_record_images.
- scheduled_maintenance.
- scheduled_maintenance_history.
- reservations.
- reservation_guests.
- reservation_supplies.
- task_lists.
- task_items.
- purchase_lists.
- purchase_items.
- documents.
- document_chunk_metadata.
- ai_chat_sessions.
- ai_chat_messages.

Regla crítica:

> El backend siempre debe filtrar por la organización del usuario autenticado.

---

## 3. Catálogos operativos

Los catálogos principales son:

- maintenance_categories.
- maintenance_types.
- maintenance_people.
- platforms.
- suppliers.
- cities.
- brands.
- task_templates.
- inventory_items.

`inventory_items` reemplaza el catálogo antiguo `materials`.

---

## 4. Inventory Items

Tabla:

```text
inventory_items
```

Propósito:

Catálogo operativo compartido para items usados en mantenimiento, compras, reservaciones y futuros reportes de inventario.

Campos principales:

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
name VARCHAR(150) NOT NULL
description TEXT
unit VARCHAR(50)
item_type VARCHAR(50) NOT NULL
internal_code VARCHAR(100)
barcode VARCHAR(100)
available_for_maintenance BOOLEAN NOT NULL DEFAULT true
available_for_reservations BOOLEAN NOT NULL DEFAULT false
available_for_purchases BOOLEAN NOT NULL DEFAULT true
status VARCHAR(50) NOT NULL
created_at TIMESTAMP WITH TIME ZONE NOT NULL
updated_at TIMESTAMP WITH TIME ZONE NOT NULL
deleted_at TIMESTAMP WITH TIME ZONE
deleted_by UUID
```

Tipos iniciales:

```text
MATERIAL
SUPPLY
AMENITY
CLEANING_SUPPLY
TOOL
OTHER
```

Regla:

`item_type` clasifica el item, pero la disponibilidad operativa se controla con flags explícitos:

```text
available_for_maintenance
available_for_reservations
available_for_purchases
```

---

## 5. Maintenance

### maintenance_records

Representa un mantenimiento realizado.

Relaciones principales:

- organization_id.
- property_id.
- category_id.
- type_id.

### maintenance_record_items

Representa items usados en un mantenimiento.

Campos principales:

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
maintenance_record_id UUID NOT NULL
inventory_item_id UUID
quantity NUMERIC(12, 2) NOT NULL
unit VARCHAR(50)
item_name_snapshot VARCHAR(150) NOT NULL
notes TEXT
created_at TIMESTAMP WITH TIME ZONE NOT NULL
updated_at TIMESTAMP WITH TIME ZONE NOT NULL
```

Razón del snapshot:

El nombre del item usado debe conservarse aunque luego el catálogo cambie.

---

## 6. Scheduled Maintenance

### scheduled_maintenance

Representa mantenimientos planeados o recurrentes.

Campos principales:

- property_id.
- category_id.
- type_id.
- scheduled_date.
- frequency.
- status.
- next_due_date.
- notes.

### scheduled_maintenance_history

Guarda trazabilidad de eventos:

- created.
- completed.
- rescheduled.
- cancelled.

---

## 7. Reservations

### reservations

Representa una reservación.

Campos principales:

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
property_id UUID NOT NULL
platform_id UUID
reservation_code VARCHAR(150)
check_in DATE NOT NULL
check_out DATE NOT NULL
supplies_delivered BOOLEAN NOT NULL DEFAULT false
observations TEXT
reservation_value NUMERIC(12, 2)
invoice_number VARCHAR(100)
invoice_series VARCHAR(100)
status VARCHAR(50) NOT NULL
created_at TIMESTAMP WITH TIME ZONE NOT NULL
updated_at TIMESTAMP WITH TIME ZONE NOT NULL
deleted_at TIMESTAMP WITH TIME ZONE
```

Regla:

`check_out` debe ser mayor que `check_in`.

### reservation_guests

Relación entre reservación y huéspedes.

Constraint recomendado:

```sql
UNIQUE (reservation_id, guest_id)
```

### reservation_supplies

Representa supplies entregados a un huésped durante una reservación.

Campos principales:

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
reservation_id UUID NOT NULL
inventory_item_id UUID NOT NULL
quantity NUMERIC(12, 2) NOT NULL
unit VARCHAR(50)
item_name_snapshot VARCHAR(150) NOT NULL
internal_code_snapshot VARCHAR(100)
barcode_snapshot VARCHAR(100)
notes TEXT
created_at TIMESTAMP WITH TIME ZONE NOT NULL
updated_at TIMESTAMP WITH TIME ZONE NOT NULL
```

Reglas:

- `quantity > 0`.
- `inventory_item_id` debe apuntar a un item disponible para reservaciones.
- El snapshot conserva trazabilidad aunque el item cambie posteriormente.

---

## 8. Purchase Lists

### purchase_lists

Representa una lista de compras.

Relaciones principales:

- property_id, opcional.
- supplier_id, opcional.
- city_id, opcional.

### purchase_items

Representa items solicitados dentro de una lista de compras.

Campos principales:

```sql
id UUID PRIMARY KEY
organization_id UUID NOT NULL
purchase_list_id UUID NOT NULL
inventory_item_id UUID
brand_id UUID
item_name_snapshot VARCHAR(150) NOT NULL
quantity NUMERIC(12, 2) NOT NULL
unit VARCHAR(50)
estimated_unit_price NUMERIC(12, 2)
purchased BOOLEAN NOT NULL DEFAULT false
notes TEXT
created_at TIMESTAMP WITH TIME ZONE NOT NULL
updated_at TIMESTAMP WITH TIME ZONE NOT NULL
```

Regla:

El item puede venir desde `inventory_items` o escribirse manualmente mediante snapshot.

---

## 9. Documents and AI

### documents

Guarda metadatos de documentos subidos a S3.

Campos principales:

- organization_id.
- property_id.
- document_type.
- original_filename.
- stored_filename.
- s3_key.
- content_type.
- size_bytes.
- processing_status.
- uploaded_by.

### document_chunk_metadata

Guarda metadatos de chunks procesados para RAG.

Los vectores se almacenan en Chroma.

### ai_chat_sessions / ai_chat_messages

Guardan historial básico de conversaciones del asistente IA.

---

## 10. Flyway

Reglas:

- No modificar migraciones ya aplicadas.
- Crear nuevas migraciones para cambios.
- Mantener SQL legible.
- Usar constraints explícitos.
- Usar índices para FK y filtros frecuentes.

Migraciones clave recientes:

```text
V20__refactor_materials_to_inventory_items.sql
V21__create_reservation_supplies.sql
```

`V20` mantiene la historia del refactor desde `materials` hacia `inventory_items`. No debe eliminarse.
