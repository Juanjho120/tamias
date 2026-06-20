ALTER TABLE product_box_model_faces
    ADD COLUMN auto_detected_points BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN contour_confidence NUMERIC(5,4),
    ADD COLUMN enhancement_mode VARCHAR(20) NOT NULL DEFAULT 'basic';
