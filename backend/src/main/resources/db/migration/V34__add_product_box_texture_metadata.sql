ALTER TABLE product_box_model_faces
    ALTER COLUMN s3_key DROP NOT NULL,
    ALTER COLUMN filepath DROP NOT NULL,
    ALTER COLUMN original_filename DROP NOT NULL,
    ALTER COLUMN content_type DROP NOT NULL,
    ALTER COLUMN size_bytes DROP NOT NULL;

ALTER TABLE product_box_model_faces
    ADD COLUMN original_s3_key VARCHAR(500) NULL,
    ADD COLUMN original_filepath VARCHAR(300) NULL,
    ADD COLUMN original_upload_filename VARCHAR(255) NULL,
    ADD COLUMN original_content_type VARCHAR(100) NULL,
    ADD COLUMN original_size_bytes BIGINT NULL,
    ADD COLUMN original_width_px INTEGER NULL,
    ADD COLUMN original_height_px INTEGER NULL,
    ADD COLUMN processed_s3_key VARCHAR(500) NULL,
    ADD COLUMN processed_filepath VARCHAR(300) NULL,
    ADD COLUMN processed_filename VARCHAR(255) NULL,
    ADD COLUMN processed_content_type VARCHAR(100) NULL,
    ADD COLUMN processed_size_bytes BIGINT NULL,
    ADD COLUMN processed_width_px INTEGER NULL,
    ADD COLUMN processed_height_px INTEGER NULL,
    ADD COLUMN target_aspect_ratio NUMERIC(12, 6) NULL,
    ADD COLUMN points_json JSONB NULL,
    ADD COLUMN texture_status VARCHAR(30) NOT NULL DEFAULT 'ACCEPTED',
    ADD COLUMN processing_error TEXT NULL,
    ADD COLUMN processed_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN accepted_at TIMESTAMP WITH TIME ZONE NULL;

UPDATE product_box_model_faces
   SET accepted_at = COALESCE(updated_at, created_at),
       texture_status = 'ACCEPTED'
 WHERE s3_key IS NOT NULL;

ALTER TABLE product_box_model_faces
    ADD CONSTRAINT chk_product_box_model_faces_texture_status
    CHECK (texture_status IN ('UPLOADED', 'POINTS_SELECTED', 'PROCESSED', 'ACCEPTED', 'FAILED')),
    ADD CONSTRAINT chk_product_box_model_faces_original_size_positive
    CHECK (original_size_bytes IS NULL OR original_size_bytes >= 0),
    ADD CONSTRAINT chk_product_box_model_faces_processed_size_positive
    CHECK (processed_size_bytes IS NULL OR processed_size_bytes >= 0),
    ADD CONSTRAINT chk_product_box_model_faces_original_width_positive
    CHECK (original_width_px IS NULL OR original_width_px > 0),
    ADD CONSTRAINT chk_product_box_model_faces_original_height_positive
    CHECK (original_height_px IS NULL OR original_height_px > 0),
    ADD CONSTRAINT chk_product_box_model_faces_processed_width_positive
    CHECK (processed_width_px IS NULL OR processed_width_px > 0),
    ADD CONSTRAINT chk_product_box_model_faces_processed_height_positive
    CHECK (processed_height_px IS NULL OR processed_height_px > 0),
    ADD CONSTRAINT chk_product_box_model_faces_target_aspect_ratio_positive
    CHECK (target_aspect_ratio IS NULL OR target_aspect_ratio > 0);

CREATE INDEX idx_product_box_model_faces_texture_status
    ON product_box_model_faces(texture_status);

CREATE INDEX idx_product_box_model_faces_original_s3_key
    ON product_box_model_faces(original_s3_key)
    WHERE original_s3_key IS NOT NULL;

CREATE INDEX idx_product_box_model_faces_processed_s3_key
    ON product_box_model_faces(processed_s3_key)
    WHERE processed_s3_key IS NOT NULL;
