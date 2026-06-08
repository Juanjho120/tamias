CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NOT NULL,
    platform_id UUID NULL,
    guest_name VARCHAR(150) NOT NULL,
    guest_email VARCHAR(150),
    guest_phone VARCHAR(50),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guests_count INTEGER NOT NULL DEFAULT 1,
    total_amount NUMERIC(12, 2) NULL,
    paid_amount NUMERIC(12, 2) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    notes TEXT,
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_reservations_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_reservations_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_reservations_platform
        FOREIGN KEY (platform_id)
        REFERENCES platforms(id),

    CONSTRAINT fk_reservations_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_reservations_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT fk_reservations_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_reservations_dates
        CHECK (check_out_date > check_in_date),

    CONSTRAINT chk_reservations_guests_count
        CHECK (guests_count >= 1),

    CONSTRAINT chk_reservations_total_amount
        CHECK (total_amount IS NULL OR total_amount >= 0),

    CONSTRAINT chk_reservations_paid_amount
        CHECK (paid_amount IS NULL OR paid_amount >= 0),

    CONSTRAINT chk_reservations_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'BLOCKED', 'DELETED')),

    CONSTRAINT chk_reservations_payment_status
        CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'REFUNDED'))
);

CREATE INDEX idx_reservations_organization_status
    ON reservations(organization_id, status);

CREATE INDEX idx_reservations_property
    ON reservations(property_id);

CREATE INDEX idx_reservations_platform
    ON reservations(platform_id);

CREATE INDEX idx_reservations_check_in
    ON reservations(check_in_date);

CREATE INDEX idx_reservations_check_out
    ON reservations(check_out_date);

CREATE INDEX idx_reservations_property_dates
    ON reservations(property_id, check_in_date, check_out_date);

CREATE TRIGGER trg_reservations_set_updated_at
BEFORE UPDATE ON reservations
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
