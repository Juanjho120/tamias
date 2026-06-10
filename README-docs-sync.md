# Bloque 5 — Documentation sync after Inventory Items and Reservation Supplies

## Purpose

This package updates project documentation after the large refactor that introduced:

- Inventory Items.
- Maintenance Record Items.
- Purchase Items using Inventory Items.
- Reservation Supplies.
- Separate Reservation Supplies modal.
- Frontend removal of legacy material aliases.

## Files included

```text
README.md
docs/01-architecture-mvp.md
docs/02-database-design-mvp.md
docs/03-api-design-mvp.md
docs/05-frontend-design-mvp.md
docs/08-inventory-items-and-reservation-supplies.md
docs/ROADMAP.md
docs/DECISIONS.md
```

## Main updates

### Replaced old terminology

Old references like:

```text
Material
Materials
MaintenanceMaterialUsed
maintenance_materials_used
/api/v1/materials
/catalogs/materials
```

were replaced with the current architecture:

```text
InventoryItem
Inventory Items
MaintenanceRecordItem
maintenance_record_items
/api/v1/inventory-items
```

### Reservation Supplies is no longer future work

The docs now describe Reservation Supplies as implemented:

```text
reservation_supplies
/api/v1/reservations/{id}/supplies
Reservation Supplies modal
```

### Roadmap updated

The roadmap now reflects:

```text
Completed:
- Inventory Items
- Maintenance Record Items
- Reservation Supplies
- RAG document search
- Dashboard calendar

Next:
- MVP hardening
- Dashboard analytics
- Production-like deployment
- AI Tool Calling
```

## Validate after applying

```bash
git diff -- README.md docs
```

Recommended search:

```bash
grep -RniE "MaintenanceMaterialUsed|maintenance_materials_used|/catalogs/materials|/api/v1/materials|materials catalog" README.md docs
```

Expected:

- No current-domain references to old materials architecture.
- Historical mentions are only acceptable when explaining the migration from `materials` to `inventory_items`.

## Commit

```bash
git add README.md docs
git commit -m "docs: sync inventory items and reservation supplies architecture"
git push
```
