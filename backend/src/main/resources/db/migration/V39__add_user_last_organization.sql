ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_organization_id UUID;

ALTER TABLE users
    ADD CONSTRAINT fk_users_last_organization
        FOREIGN KEY (last_organization_id)
        REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_users_last_organization_id
    ON users(last_organization_id);
