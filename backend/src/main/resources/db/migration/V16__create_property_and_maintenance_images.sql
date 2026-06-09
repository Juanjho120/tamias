CREATE TABLE property_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_property_images_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_property_images_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_property_images_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_property_images_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_property_images_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),

    CONSTRAINT chk_property_images_size_bytes
        CHECK (size_bytes >= 0)
);

CREATE TABLE maintenance_record_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    maintenance_record_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_maintenance_record_images_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_maintenance_record_images_record
        FOREIGN KEY (maintenance_record_id)
        REFERENCES maintenance_records(id),

    CONSTRAINT fk_maintenance_record_images_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_maintenance_record_images_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_maintenance_record_images_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),

    CONSTRAINT chk_maintenance_record_images_size_bytes
        CHECK (size_bytes >= 0)
);

CREATE INDEX idx_property_images_property
    ON property_images(property_id);

CREATE INDEX idx_property_images_organization
    ON property_images(organization_id);

CREATE INDEX idx_property_images_org_status
    ON property_images(organization_id, status);

CREATE INDEX idx_property_images_cover
    ON property_images(property_id, is_cover);

CREATE INDEX idx_maintenance_record_images_record
    ON maintenance_record_images(maintenance_record_id);

CREATE INDEX idx_maintenance_record_images_organization
    ON maintenance_record_images(organization_id);

CREATE INDEX idx_maintenance_record_images_org_status
    ON maintenance_record_images(organization_id, status);
