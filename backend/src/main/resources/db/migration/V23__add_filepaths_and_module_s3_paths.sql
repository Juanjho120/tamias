ALTER TABLE documents
    ADD COLUMN filepath VARCHAR(300) NULL;

ALTER TABLE property_images
    ADD COLUMN filepath VARCHAR(300) NULL;

ALTER TABLE maintenance_record_images
    ADD COLUMN filepath VARCHAR(300) NULL;
