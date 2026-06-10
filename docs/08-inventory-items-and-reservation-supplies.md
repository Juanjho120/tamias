# Inventory Items and Reservation Supplies

## Purpose

TAMIAS needs a clean shared catalog for operational items used across maintenance, purchases, reservations and future inventory reporting.

The original MVP used `materials` as a catalog. That worked for maintenance and purchases, but it does not scale cleanly because the same kind of item can be:

- used as a maintenance material,
- purchased in a purchase list,
- delivered to a guest during a reservation,
- tracked later with an internal code or barcode.

Because of this, TAMIAS will refactor `materials` into a broader concept:

```text
Inventory Items
```

## Business naming

Recommended UI labels:

| Language | Label |
|---|---|
| Spanish | Insumos y materiales |
| English | Inventory Items |

`Suministros` is understandable, but `Insumos` is more natural for the type of operational items TAMIAS needs to manage.

## Technical decision

TAMIAS will use:

```text
inventory_items
```

as the shared operational item catalog.

Existing `materials` will be renamed to `inventory_items`.

Existing maintenance item usage will be represented by:

```text
maintenance_record_items
```

Instead of older names such as:

```text
maintenance_materials_used
maintenance_record_materials
```

## Inventory item fields

```text
id
organization_id
name
description
unit
item_type
internal_code
barcode
available_for_maintenance
available_for_reservations
available_for_purchases
status
created_at
updated_at
deleted_at
```

## Item types

Initial supported item types:

```text
MATERIAL
SUPPLY
AMENITY
CLEANING_SUPPLY
TOOL
OTHER
```

## Availability flags

The item type does not fully define where the item can be used. Availability is controlled by explicit flags:

```text
available_for_maintenance
available_for_reservations
available_for_purchases
```

## API direction

The new main catalog API is:

```http
GET    /api/v1/inventory-items
GET    /api/v1/inventory-items/{id}
POST   /api/v1/inventory-items
PUT    /api/v1/inventory-items/{id}
DELETE /api/v1/inventory-items/{id}
```

Supported filters:

```http
/api/v1/inventory-items?status=ACTIVE
/api/v1/inventory-items?itemType=SUPPLY
/api/v1/inventory-items?availableForMaintenance=true
/api/v1/inventory-items?availableForReservations=true
/api/v1/inventory-items?availableForPurchases=true
/api/v1/inventory-items?search=shampoo
```

A temporary compatibility endpoint may remain during frontend migration:

```http
/api/v1/catalogs/materials
```

but the frontend should move to `/api/v1/inventory-items`.

## Maintenance items

Maintenance records should use:

```http
GET    /api/v1/maintenance-records/{maintenanceRecordId}/items
POST   /api/v1/maintenance-records/{maintenanceRecordId}/items
PUT    /api/v1/maintenance-records/{maintenanceRecordId}/items/{itemId}
DELETE /api/v1/maintenance-records/{maintenanceRecordId}/items/{itemId}
```

Temporary compatibility endpoints may remain:

```http
/api/v1/maintenance-records/{maintenanceRecordId}/materials
```

## Purchase items

Purchase items now reference:

```text
inventory_item_id
```

The frontend should eventually rename `materialId` to `inventoryItemId`.

During the transition, the backend can accept both names.

## Future reservation supplies

Reservation supplies should be implemented in a later block using:

```text
reservation_supplies
```

Each supply will reference an `inventory_item_id`.

Suggested fields:

```text
id
organization_id
reservation_id
inventory_item_id
quantity
unit
item_name_snapshot
internal_code_snapshot
barcode_snapshot
unit_cost_snapshot
notes
created_at
updated_at
```

## Scope of this block

This block covers:

```text
Documentation
Backend InventoryItem refactor
materials -> inventory_items
maintenance material table -> maintenance_record_items
purchase_items.material_id -> purchase_items.inventory_item_id
```

This block does not implement:

```text
Frontend inventory items refactor
Reservation supplies UI
Stock control
Inventory movements
```
