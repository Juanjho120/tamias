CREATE TABLE IF NOT EXISTS reservation_supplies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    reservation_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    quantity NUMERIC(12, 2) NOT NULL,
    unit VARCHAR(50),
    item_name_snapshot VARCHAR(150) NOT NULL,
    internal_code_snapshot VARCHAR(100),
    barcode_snapshot VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_reservation_supplies_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_reservation_supplies_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT fk_reservation_supplies_inventory_item
        FOREIGN KEY (inventory_item_id)
        REFERENCES inventory_items(id),

    CONSTRAINT chk_reservation_supplies_quantity
        CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_reservation_supplies_organization
    ON reservation_supplies(organization_id);

CREATE INDEX IF NOT EXISTS idx_reservation_supplies_reservation
    ON reservation_supplies(reservation_id);

CREATE INDEX IF NOT EXISTS idx_reservation_supplies_inventory_item
    ON reservation_supplies(inventory_item_id);
