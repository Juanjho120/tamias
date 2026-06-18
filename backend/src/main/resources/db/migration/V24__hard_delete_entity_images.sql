-- 11B — Entity images use hard delete.
-- Existing soft-deleted metadata rows are removed from PostgreSQL before the soft-delete columns are dropped.
-- Existing orphaned S3 objects from older soft deletes cannot be removed safely from a DB migration.

DELETE FROM property_images
WHERE status = 'DELETED'
   OR deleted_at IS NOT NULL;

DELETE FROM maintenance_record_images
WHERE status = 'DELETED'
   OR deleted_at IS NOT NULL;

ALTER TABLE property_images
    DROP CONSTRAINT IF EXISTS fk_property_images_deleted_by;

ALTER TABLE maintenance_record_images
    DROP CONSTRAINT IF EXISTS fk_maintenance_record_images_deleted_by;

ALTER TABLE property_images
    DROP COLUMN IF EXISTS deleted_by,
    DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE maintenance_record_images
    DROP COLUMN IF EXISTS deleted_by,
    DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE property_images
    DROP CONSTRAINT IF EXISTS chk_property_images_status;

ALTER TABLE property_images
    ADD CONSTRAINT chk_property_images_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE maintenance_record_images
    DROP CONSTRAINT IF EXISTS chk_maintenance_record_images_status;

ALTER TABLE maintenance_record_images
    ADD CONSTRAINT chk_maintenance_record_images_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'));
