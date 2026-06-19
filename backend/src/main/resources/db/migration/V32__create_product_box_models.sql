CREATE TABLE product_box_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    inventory_item_id UUID NULL,
    purchase_item_id UUID NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    width NUMERIC(10, 2) NOT NULL,
    height NUMERIC(10, 2) NOT NULL,
    depth NUMERIC(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'cm',
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_product_box_models_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_product_box_models_inventory_item
        FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_product_box_models_purchase_item
        FOREIGN KEY (purchase_item_id) REFERENCES purchase_items(id),
    CONSTRAINT fk_product_box_models_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_product_box_models_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_product_box_models_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES users(id),

    CONSTRAINT chk_product_box_models_width_positive CHECK (width > 0),
    CONSTRAINT chk_product_box_models_height_positive CHECK (height > 0),
    CONSTRAINT chk_product_box_models_depth_positive CHECK (depth > 0),
    CONSTRAINT chk_product_box_models_unit CHECK (unit IN ('cm', 'mm', 'in'))
);

CREATE INDEX idx_product_box_models_organization_id
    ON product_box_models(organization_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_product_box_models_inventory_item_id
    ON product_box_models(inventory_item_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_product_box_models_purchase_item_id
    ON product_box_models(purchase_item_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_product_box_models_organization_name
    ON product_box_models(organization_id, name)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_product_box_models_set_updated_at
    BEFORE UPDATE ON product_box_models
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
