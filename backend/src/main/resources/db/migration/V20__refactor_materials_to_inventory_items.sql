-- Refactor materials into inventory_items.
-- Defensive migration because earlier development branches may have used
-- slightly different maintenance item table names.

ALTER TABLE IF EXISTS materials RENAME TO inventory_items;

ALTER TABLE IF EXISTS inventory_items
    ADD COLUMN IF NOT EXISTS item_type VARCHAR(50) NOT NULL DEFAULT 'MATERIAL',
    ADD COLUMN IF NOT EXISTS internal_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(100),
    ADD COLUMN IF NOT EXISTS available_for_maintenance BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS available_for_reservations BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS available_for_purchases BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE IF EXISTS inventory_items
    DROP CONSTRAINT IF EXISTS chk_inventory_items_item_type;

ALTER TABLE IF EXISTS inventory_items
    ADD CONSTRAINT chk_inventory_items_item_type
        CHECK (item_type IN ('MATERIAL', 'SUPPLY', 'AMENITY', 'CLEANING_SUPPLY', 'TOOL', 'OTHER'));

CREATE INDEX IF NOT EXISTS idx_inventory_items_org_status
    ON inventory_items(organization_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_items_org_type
    ON inventory_items(organization_id, item_type);

CREATE INDEX IF NOT EXISTS idx_inventory_items_org_internal_code
    ON inventory_items(organization_id, internal_code);

CREATE INDEX IF NOT EXISTS idx_inventory_items_org_barcode
    ON inventory_items(organization_id, barcode);

ALTER TABLE IF EXISTS purchase_items
    DROP CONSTRAINT IF EXISTS fk_purchase_items_material;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'purchase_items'
          AND column_name = 'material_id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'purchase_items'
          AND column_name = 'inventory_item_id'
    ) THEN
        ALTER TABLE purchase_items RENAME COLUMN material_id TO inventory_item_id;
    END IF;
END $$;

DROP INDEX IF EXISTS idx_purchase_items_material;

CREATE INDEX IF NOT EXISTS idx_purchase_items_inventory_item
    ON purchase_items(inventory_item_id);

ALTER TABLE IF EXISTS purchase_items
    DROP CONSTRAINT IF EXISTS fk_purchase_items_inventory_item;

ALTER TABLE IF EXISTS purchase_items
    ADD CONSTRAINT fk_purchase_items_inventory_item
        FOREIGN KEY (inventory_item_id)
        REFERENCES inventory_items(id);

DO $$
BEGIN
    IF to_regclass('public.maintenance_record_items') IS NULL
       AND to_regclass('public.maintenance_materials_used') IS NOT NULL THEN
        ALTER TABLE maintenance_materials_used RENAME TO maintenance_record_items;
    ELSIF to_regclass('public.maintenance_record_items') IS NULL
       AND to_regclass('public.maintenance_record_materials') IS NOT NULL THEN
        ALTER TABLE maintenance_record_materials RENAME TO maintenance_record_items;
    END IF;
END $$;

ALTER TABLE IF EXISTS maintenance_record_items
    DROP CONSTRAINT IF EXISTS fk_maintenance_materials_used_material,
    DROP CONSTRAINT IF EXISTS fk_maintenance_record_materials_material,
    DROP CONSTRAINT IF EXISTS fk_maintenance_record_items_inventory_item;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'maintenance_record_items'
          AND column_name = 'material_id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'maintenance_record_items'
          AND column_name = 'inventory_item_id'
    ) THEN
        ALTER TABLE maintenance_record_items RENAME COLUMN material_id TO inventory_item_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'maintenance_record_items'
          AND column_name = 'material_name_snapshot'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'maintenance_record_items'
          AND column_name = 'item_name_snapshot'
    ) THEN
        ALTER TABLE maintenance_record_items RENAME COLUMN material_name_snapshot TO item_name_snapshot;
    END IF;
END $$;

DROP INDEX IF EXISTS idx_maintenance_materials_material;
DROP INDEX IF EXISTS idx_maintenance_materials_record;
DROP INDEX IF EXISTS idx_maintenance_materials_organization;
DROP INDEX IF EXISTS idx_maintenance_record_materials_material;
DROP INDEX IF EXISTS idx_maintenance_record_materials_record;
DROP INDEX IF EXISTS idx_maintenance_record_materials_organization;

CREATE INDEX IF NOT EXISTS idx_maintenance_record_items_inventory_item
    ON maintenance_record_items(inventory_item_id);

CREATE INDEX IF NOT EXISTS idx_maintenance_record_items_record
    ON maintenance_record_items(maintenance_record_id);

CREATE INDEX IF NOT EXISTS idx_maintenance_record_items_organization
    ON maintenance_record_items(organization_id);

ALTER TABLE IF EXISTS maintenance_record_items
    ADD CONSTRAINT fk_maintenance_record_items_inventory_item
        FOREIGN KEY (inventory_item_id)
        REFERENCES inventory_items(id);
