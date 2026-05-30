CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_properties_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_properties_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_properties_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT fk_properties_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT uk_properties_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_properties_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_properties_organization
    ON properties(organization_id);

CREATE INDEX idx_properties_organization_status
    ON properties(organization_id, status);

CREATE INDEX idx_properties_created_by
    ON properties(created_by);

CREATE INDEX idx_properties_updated_by
    ON properties(updated_by);

CREATE INDEX idx_properties_deleted_by
    ON properties(deleted_by);

CREATE TRIGGER trg_properties_set_updated_at
BEFORE UPDATE ON properties
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
