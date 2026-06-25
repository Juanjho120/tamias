ALTER TABLE IF EXISTS maintenance_record_images
    ADD COLUMN IF NOT EXISTS image_role VARCHAR(20) NOT NULL DEFAULT 'GENERAL';

ALTER TABLE IF EXISTS maintenance_record_images
    DROP CONSTRAINT IF EXISTS chk_maintenance_record_images_image_role;

ALTER TABLE IF EXISTS maintenance_record_images
    ADD CONSTRAINT chk_maintenance_record_images_image_role
    CHECK (image_role IN ('BEFORE', 'AFTER', 'GENERAL'));

CREATE INDEX IF NOT EXISTS idx_maintenance_record_images_role
    ON maintenance_record_images(maintenance_record_id, image_role);

CREATE TABLE IF NOT EXISTS maintenance_record_serviced_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    maintenance_record_id UUID NOT NULL,
    inventory_item_id UUID NULL,
    item_name_snapshot VARCHAR(150) NOT NULL,
    quantity NUMERIC(12, 2),
    unit VARCHAR(50),
    notes TEXT,
    created_by UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by UUID NULL,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_maintenance_record_serviced_items_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_maintenance_record_serviced_items_record
        FOREIGN KEY (maintenance_record_id) REFERENCES maintenance_records(id),
    CONSTRAINT fk_maintenance_record_serviced_items_inventory_item
        FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_maintenance_record_serviced_items_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_maintenance_record_serviced_items_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_maintenance_record_serviced_items_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES users(id),
    CONSTRAINT chk_maintenance_record_serviced_items_quantity
        CHECK (quantity IS NULL OR quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_maintenance_record_serviced_items_record
    ON maintenance_record_serviced_items(maintenance_record_id);

CREATE INDEX IF NOT EXISTS idx_maintenance_record_serviced_items_inventory_item
    ON maintenance_record_serviced_items(inventory_item_id);

CREATE INDEX IF NOT EXISTS idx_maintenance_record_serviced_items_organization
    ON maintenance_record_serviced_items(organization_id);

CREATE INDEX IF NOT EXISTS idx_maintenance_record_serviced_items_deleted_at
    ON maintenance_record_serviced_items(deleted_at);
