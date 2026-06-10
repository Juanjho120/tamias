# Inventory Items and Reservation Supplies

## Purpose

TAMIAS uses a clean shared catalog for operational items used across maintenance, purchases, reservations and future inventory reporting.

The original MVP used `materials` as a catalog. That worked for maintenance and purchases, but it did not scale cleanly because the same kind of item can be:

- used as a maintenance material,
- purchased in a purchase list,
- delivered to a guest during a reservation,
- tracked later with an internal code or barcode,
- used in future analytics and AI tool calling.

Because of this, TAMIAS refactored `materials` into a broader concept:

```text
Inventory Items
```

---

## Business naming

Recommended UI labels:

| Language | Label |
|---|---|
| Spanish | Insumos y materiales |
| English | Inventory Items |

`Insumos` is more natural for the operational items TAMIAS needs to manage.

---

## Technical decision

TAMIAS uses:

```text
inventory_items
```

as the shared operational item catalog.

The old `materials` concept was replaced by:

```text
InventoryItem
```

---

## Current implemented model

### inventory_items

Main fields:

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

Initial supported item types:

```text
MATERIAL
SUPPLY
AMENITY
CLEANING_SUPPLY
TOOL
OTHER
```

Availability is controlled by explicit flags:

```text
available_for_maintenance
available_for_reservations
available_for_purchases
```

The item type classifies the item, but it does not fully define where it can be used.

---

## API

Main catalog API:

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

---

## Maintenance Items

Maintenance records use:

```text
maintenance_record_items
```

Endpoints:

```http
GET    /api/v1/maintenance-records/{maintenanceRecordId}/items
POST   /api/v1/maintenance-records/{maintenanceRecordId}/items
PUT    /api/v1/maintenance-records/{maintenanceRecordId}/items/{itemId}
DELETE /api/v1/maintenance-records/{maintenanceRecordId}/items/{itemId}
```

Each item references `inventory_items` when available and stores a snapshot of the item name for historical traceability.

---

## Purchase Items

Purchase items reference:

```text
inventory_item_id
```

The frontend now uses:

```text
inventoryItemId
inventoryItemName
```

instead of legacy material aliases.

Purchase items can still store manual item names through snapshots when needed.

---

## Reservation Supplies

Reservation supplies are implemented using:

```text
reservation_supplies
```

Each supply references:

```text
inventory_item_id
```

and stores snapshots:

```text
item_name_snapshot
internal_code_snapshot
barcode_snapshot
```

Endpoints:

```http
GET    /api/v1/reservations/{reservationId}/supplies
POST   /api/v1/reservations/{reservationId}/supplies
PUT    /api/v1/reservations/{reservationId}/supplies/{supplyId}
DELETE /api/v1/reservations/{reservationId}/supplies/{supplyId}
```

Frontend behavior:

- Supplies are not edited inside the main reservation form.
- Supplies use a dedicated modal opened from the reservation table.
- The modal loads inventory items using `availableForReservations=true`.

---

## Current scope completed

This refactor completed:

```text
Documentation sync
Backend InventoryItem refactor
materials -> inventory_items
maintenance item usage -> maintenance_record_items
purchase_items.material_id -> purchase_items.inventory_item_id
Frontend Inventory Items refactor
Reservation Supplies backend
Reservation Supplies frontend modal
Frontend legacy material aliases cleanup
```

---

## Not included yet

The refactor does not implement formal stock control.

Future inventory features may include:

```text
inventory_movements
stock_on_hand
stock adjustments
low stock alerts
consumption analytics
AI tool calling for inventory usage
```
