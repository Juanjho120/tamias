CREATE TABLE inventory_item_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    filepath VARCHAR(300),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventory_item_images_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),
    CONSTRAINT fk_inventory_item_images_inventory_item
        FOREIGN KEY (inventory_item_id)
        REFERENCES inventory_items(id),
    CONSTRAINT fk_inventory_item_images_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),
    CONSTRAINT chk_inventory_item_images_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT chk_inventory_item_images_size_bytes
        CHECK (size_bytes >= 0)
);

CREATE INDEX idx_inventory_item_images_inventory_item_id
    ON inventory_item_images(inventory_item_id);

CREATE INDEX idx_inventory_item_images_organization_id
    ON inventory_item_images(organization_id);

CREATE INDEX idx_inventory_item_images_org_status
    ON inventory_item_images(organization_id, status);

CREATE INDEX idx_inventory_item_images_item_cover
    ON inventory_item_images(inventory_item_id, is_cover);

CREATE INDEX idx_inventory_item_images_org_item_status
    ON inventory_item_images(organization_id, inventory_item_id, status);
