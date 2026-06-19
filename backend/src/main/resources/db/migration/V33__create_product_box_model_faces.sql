CREATE TABLE product_box_model_faces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    product_box_model_id UUID NOT NULL,
    face_name VARCHAR(20) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    filepath VARCHAR(300) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    rotation_degrees NUMERIC(10, 2) NULL,
    flip_horizontal BOOLEAN NOT NULL DEFAULT false,
    flip_vertical BOOLEAN NOT NULL DEFAULT false,
    created_by UUID NOT NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_product_box_model_faces_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_product_box_model_faces_model
        FOREIGN KEY (product_box_model_id) REFERENCES product_box_models(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_box_model_faces_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_product_box_model_faces_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),

    CONSTRAINT chk_product_box_model_faces_face_name
        CHECK (face_name IN ('front', 'back', 'left', 'right', 'top', 'bottom')),
    CONSTRAINT chk_product_box_model_faces_size_positive
        CHECK (size_bytes >= 0),
    CONSTRAINT ux_product_box_model_faces_model_face
        UNIQUE (product_box_model_id, face_name)
);

CREATE INDEX idx_product_box_model_faces_organization_id
    ON product_box_model_faces(organization_id);

CREATE INDEX idx_product_box_model_faces_model_id
    ON product_box_model_faces(product_box_model_id);

CREATE INDEX idx_product_box_model_faces_model_face
    ON product_box_model_faces(product_box_model_id, face_name);

CREATE TRIGGER trg_product_box_model_faces_set_updated_at
    BEFORE UPDATE ON product_box_model_faces
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
