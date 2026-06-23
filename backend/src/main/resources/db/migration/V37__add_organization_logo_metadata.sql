ALTER TABLE organizations
  ADD COLUMN logo_original_filename VARCHAR(255) NULL,
  ADD COLUMN logo_s3_key TEXT NULL,
  ADD COLUMN logo_filepath VARCHAR(300) NULL,
  ADD COLUMN logo_content_type VARCHAR(100) NULL,
  ADD COLUMN logo_size_bytes BIGINT NULL,
  ADD COLUMN logo_updated_at TIMESTAMP WITH TIME ZONE NULL;

ALTER TABLE organizations
  ADD CONSTRAINT chk_organizations_logo_size_bytes
  CHECK (logo_size_bytes IS NULL OR logo_size_bytes >= 0);
