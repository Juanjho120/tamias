CREATE TABLE scheduled_maintenance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NOT NULL,
    maintenance_category_id UUID NULL,
    maintenance_type_id UUID NULL,
    maintenance_person_id UUID NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    frequency VARCHAR(30) NOT NULL,
    interval_value INTEGER NOT NULL DEFAULT 1,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    next_due_date DATE NOT NULL,
    last_generated_at TIMESTAMP WITH TIME ZONE NULL,
    estimated_cost NUMERIC(12, 2) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_scheduled_maintenance_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_scheduled_maintenance_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_scheduled_maintenance_category FOREIGN KEY (maintenance_category_id) REFERENCES maintenance_categories(id),
    CONSTRAINT fk_scheduled_maintenance_type FOREIGN KEY (maintenance_type_id) REFERENCES maintenance_types(id),
    CONSTRAINT fk_scheduled_maintenance_person FOREIGN KEY (maintenance_person_id) REFERENCES maintenance_people(id),
    CONSTRAINT fk_scheduled_maintenance_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_scheduled_maintenance_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_scheduled_maintenance_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id),
    CONSTRAINT chk_scheduled_maintenance_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT chk_scheduled_maintenance_interval CHECK (interval_value >= 1),
    CONSTRAINT chk_scheduled_maintenance_estimated_cost CHECK (estimated_cost IS NULL OR estimated_cost >= 0),
    CONSTRAINT chk_scheduled_maintenance_status CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'DELETED'))
);

CREATE INDEX idx_scheduled_maintenance_organization_status ON scheduled_maintenance(organization_id, status);
CREATE INDEX idx_scheduled_maintenance_property ON scheduled_maintenance(property_id);
CREATE INDEX idx_scheduled_maintenance_category ON scheduled_maintenance(maintenance_category_id);
CREATE INDEX idx_scheduled_maintenance_type ON scheduled_maintenance(maintenance_type_id);
CREATE INDEX idx_scheduled_maintenance_person ON scheduled_maintenance(maintenance_person_id);
CREATE INDEX idx_scheduled_maintenance_next_due_date ON scheduled_maintenance(next_due_date);

CREATE TRIGGER trg_scheduled_maintenance_set_updated_at
BEFORE UPDATE ON scheduled_maintenance
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
