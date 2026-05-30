CREATE TABLE maintenance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NOT NULL,
    maintenance_type_id UUID NULL,
    maintenance_person_id UUID NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    scheduled_at TIMESTAMP WITH TIME ZONE NULL,
    performed_at TIMESTAMP WITH TIME ZONE NULL,
    cost NUMERIC(12, 2) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_maintenance_records_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_maintenance_records_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_maintenance_records_type
        FOREIGN KEY (maintenance_type_id)
        REFERENCES maintenance_types(id),

    CONSTRAINT fk_maintenance_records_person
        FOREIGN KEY (maintenance_person_id)
        REFERENCES maintenance_people(id),

    CONSTRAINT fk_maintenance_records_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_maintenance_records_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT fk_maintenance_records_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_maintenance_records_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'DELETED')),

    CONSTRAINT chk_maintenance_records_cost
        CHECK (cost IS NULL OR cost >= 0)
);

CREATE INDEX idx_maintenance_records_organization_status
    ON maintenance_records(organization_id, status);

CREATE INDEX idx_maintenance_records_property
    ON maintenance_records(property_id);

CREATE INDEX idx_maintenance_records_type
    ON maintenance_records(maintenance_type_id);

CREATE INDEX idx_maintenance_records_person
    ON maintenance_records(maintenance_person_id);

CREATE INDEX idx_maintenance_records_scheduled_at
    ON maintenance_records(scheduled_at);

CREATE INDEX idx_maintenance_records_performed_at
    ON maintenance_records(performed_at);

CREATE TRIGGER trg_maintenance_records_set_updated_at
BEFORE UPDATE ON maintenance_records
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
