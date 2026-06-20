ALTER TABLE product_box_model_faces
    ADD COLUMN ai_enhanced_s3_key VARCHAR(500),
    ADD COLUMN ai_enhanced_filepath VARCHAR(300),
    ADD COLUMN ai_enhanced_filename VARCHAR(255),
    ADD COLUMN ai_enhanced_content_type VARCHAR(100),
    ADD COLUMN ai_enhanced_size_bytes BIGINT,
    ADD COLUMN ai_enhanced_width_px INTEGER,
    ADD COLUMN ai_enhanced_height_px INTEGER,
    ADD COLUMN ai_enhancement_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUESTED',
    ADD COLUMN ai_enhancement_provider VARCHAR(80),
    ADD COLUMN ai_enhancement_model VARCHAR(120),
    ADD COLUMN ai_enhancement_prompt_version VARCHAR(80),
    ADD COLUMN ai_enhancement_error TEXT,
    ADD COLUMN ai_enhanced_at TIMESTAMP,
    ADD COLUMN active_texture_source VARCHAR(30) NOT NULL DEFAULT 'unknown';

CREATE INDEX idx_product_box_model_faces_ai_status
    ON product_box_model_faces(ai_enhancement_status);

CREATE INDEX idx_product_box_model_faces_active_texture_source
    ON product_box_model_faces(active_texture_source);
