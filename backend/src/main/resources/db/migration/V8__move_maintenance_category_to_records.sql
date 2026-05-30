ALTER TABLE maintenance_records
ADD COLUMN maintenance_category_id UUID NULL;

ALTER TABLE maintenance_records
ADD CONSTRAINT fk_maintenance_records_category
    FOREIGN KEY (maintenance_category_id)
    REFERENCES maintenance_categories(id);

CREATE INDEX idx_maintenance_records_category
    ON maintenance_records(maintenance_category_id);

ALTER TABLE maintenance_types
DROP CONSTRAINT IF EXISTS fk_maintenance_types_category;

DROP INDEX IF EXISTS idx_maintenance_types_category;

ALTER TABLE maintenance_types
DROP COLUMN IF EXISTS maintenance_category_id;
