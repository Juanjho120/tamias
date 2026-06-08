CREATE TABLE purchase_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NULL,
    city_id UUID NULL,
    supplier_id UUID NULL,
    purchase_date DATE NOT NULL,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_purchase_lists_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_purchase_lists_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_purchase_lists_city
        FOREIGN KEY (city_id)
        REFERENCES cities(id),

    CONSTRAINT fk_purchase_lists_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id),

    CONSTRAINT fk_purchase_lists_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_purchase_lists_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT fk_purchase_lists_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_purchase_lists_status
        CHECK (status IN ('OPEN', 'PARTIALLY_PURCHASED', 'COMPLETED', 'CANCELLED', 'DELETED'))
);

CREATE TABLE purchase_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    purchase_list_id UUID NOT NULL,
    material_id UUID NULL,
    brand_id UUID NULL,
    item_name_snapshot VARCHAR(150) NOT NULL,
    quantity NUMERIC(12, 2) NOT NULL DEFAULT 1,
    unit VARCHAR(50),
    estimated_price NUMERIC(12, 2),
    purchased BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_purchase_items_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_purchase_items_purchase_list
        FOREIGN KEY (purchase_list_id)
        REFERENCES purchase_lists(id),

    CONSTRAINT fk_purchase_items_material
        FOREIGN KEY (material_id)
        REFERENCES materials(id),

    CONSTRAINT fk_purchase_items_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands(id),

    CONSTRAINT chk_purchase_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_purchase_items_estimated_price
        CHECK (estimated_price IS NULL OR estimated_price >= 0)
);

CREATE INDEX idx_purchase_lists_org_date
    ON purchase_lists(organization_id, purchase_date);

CREATE INDEX idx_purchase_lists_supplier
    ON purchase_lists(supplier_id);

CREATE INDEX idx_purchase_lists_city
    ON purchase_lists(city_id);

CREATE INDEX idx_purchase_lists_property
    ON purchase_lists(property_id);

CREATE INDEX idx_purchase_lists_org_status
    ON purchase_lists(organization_id, status);

CREATE INDEX idx_purchase_items_list
    ON purchase_items(purchase_list_id);

CREATE INDEX idx_purchase_items_material
    ON purchase_items(material_id);

CREATE INDEX idx_purchase_items_brand
    ON purchase_items(brand_id);

CREATE INDEX idx_purchase_items_org_purchased
    ON purchase_items(organization_id, purchased);

CREATE TRIGGER trg_purchase_lists_set_updated_at
BEFORE UPDATE ON purchase_lists
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_purchase_items_set_updated_at
BEFORE UPDATE ON purchase_items
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
