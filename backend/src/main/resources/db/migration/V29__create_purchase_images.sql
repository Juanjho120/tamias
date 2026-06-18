CREATE TABLE purchase_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    purchase_list_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    filepath VARCHAR(300),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_purchase_images_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_purchase_images_purchase_list FOREIGN KEY (purchase_list_id) REFERENCES purchase_lists(id),
    CONSTRAINT fk_purchase_images_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_purchase_images_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_purchase_images_size_bytes CHECK (size_bytes >= 0)
);

CREATE INDEX idx_purchase_images_purchase_list ON purchase_images(purchase_list_id);
CREATE INDEX idx_purchase_images_organization ON purchase_images(organization_id);
CREATE INDEX idx_purchase_images_org_status ON purchase_images(organization_id, status);
CREATE INDEX idx_purchase_images_org_purchase_list_status ON purchase_images(organization_id, purchase_list_id, status);
