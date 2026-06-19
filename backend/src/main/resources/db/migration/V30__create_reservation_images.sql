CREATE TABLE reservation_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    reservation_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    filepath VARCHAR(300),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reservation_images_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_reservation_images_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT fk_reservation_images_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_reservation_images_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_reservation_images_size_bytes CHECK (size_bytes >= 0)
);

CREATE INDEX idx_reservation_images_reservation ON reservation_images(reservation_id);
CREATE INDEX idx_reservation_images_organization ON reservation_images(organization_id);
CREATE INDEX idx_reservation_images_org_status ON reservation_images(organization_id, status);
CREATE INDEX idx_reservation_images_org_reservation_status ON reservation_images(organization_id, reservation_id, status);
