CREATE TABLE IF NOT EXISTS guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_guests_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_guests_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_guests_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT fk_guests_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_guests_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_guests_organization
    ON guests(organization_id);

CREATE INDEX IF NOT EXISTS idx_guests_org_name
    ON guests(organization_id, full_name);

CREATE TRIGGER trg_guests_set_updated_at
BEFORE UPDATE ON guests
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS reservation_code VARCHAR(150);

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS check_in DATE;

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS check_out DATE;

UPDATE reservations
SET check_in = check_in_date
WHERE check_in IS NULL
  AND EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'reservations'
        AND column_name = 'check_in_date'
  );

UPDATE reservations
SET check_out = check_out_date
WHERE check_out IS NULL
  AND EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'reservations'
        AND column_name = 'check_out_date'
  );

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS supplies_delivered BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS observations TEXT;

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS reservation_value NUMERIC(12, 2);

UPDATE reservations
SET reservation_value = total_amount
WHERE reservation_value IS NULL
  AND EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'reservations'
        AND column_name = 'total_amount'
  );

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(100);

ALTER TABLE reservations
ADD COLUMN IF NOT EXISTS invoice_series VARCHAR(100);

ALTER TABLE reservations
ALTER COLUMN check_in SET NOT NULL;

ALTER TABLE reservations
ALTER COLUMN check_out SET NOT NULL;

ALTER TABLE reservations
DROP CONSTRAINT IF EXISTS chk_reservations_dates;

ALTER TABLE reservations
ADD CONSTRAINT chk_reservations_dates
    CHECK (check_out > check_in);

ALTER TABLE reservations
DROP CONSTRAINT IF EXISTS chk_reservations_status;

ALTER TABLE reservations
ADD CONSTRAINT chk_reservations_status
    CHECK (status IN ('ACTIVE', 'CANCELLED', 'DELETED'));

ALTER TABLE reservations
DROP CONSTRAINT IF EXISTS chk_reservations_reservation_value;

ALTER TABLE reservations
ADD CONSTRAINT chk_reservations_reservation_value
    CHECK (reservation_value IS NULL OR reservation_value >= 0);

DROP INDEX IF EXISTS idx_reservations_check_in;
DROP INDEX IF EXISTS idx_reservations_check_out;
DROP INDEX IF EXISTS idx_reservations_property_dates;

CREATE INDEX IF NOT EXISTS idx_reservations_org_property
    ON reservations(organization_id, property_id);

CREATE INDEX IF NOT EXISTS idx_reservations_org_dates
    ON reservations(organization_id, check_in, check_out);

CREATE INDEX IF NOT EXISTS idx_reservations_platform
    ON reservations(platform_id);

ALTER TABLE reservations
DROP COLUMN IF EXISTS guest_name;

ALTER TABLE reservations
DROP COLUMN IF EXISTS guest_email;

ALTER TABLE reservations
DROP COLUMN IF EXISTS guest_phone;

ALTER TABLE reservations
DROP COLUMN IF EXISTS check_in_date;

ALTER TABLE reservations
DROP COLUMN IF EXISTS check_out_date;

ALTER TABLE reservations
DROP COLUMN IF EXISTS guests_count;

ALTER TABLE reservations
DROP COLUMN IF EXISTS total_amount;

ALTER TABLE reservations
DROP COLUMN IF EXISTS paid_amount;

ALTER TABLE reservations
DROP COLUMN IF EXISTS payment_status;

CREATE TABLE IF NOT EXISTS reservation_guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    reservation_id UUID NOT NULL,
    guest_id UUID NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_reservation_guests_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_reservation_guests_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT fk_reservation_guests_guest
        FOREIGN KEY (guest_id)
        REFERENCES guests(id),

    CONSTRAINT uk_reservation_guests
        UNIQUE (reservation_id, guest_id)
);

CREATE INDEX IF NOT EXISTS idx_reservation_guests_reservation
    ON reservation_guests(reservation_id);

CREATE INDEX IF NOT EXISTS idx_reservation_guests_guest
    ON reservation_guests(guest_id);
