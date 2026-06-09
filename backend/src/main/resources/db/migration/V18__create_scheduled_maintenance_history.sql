ALTER TABLE scheduled_maintenance
DROP CONSTRAINT IF EXISTS chk_scheduled_maintenance_status;

ALTER TABLE scheduled_maintenance
ADD CONSTRAINT chk_scheduled_maintenance_status
    CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED', 'DELETED'));

CREATE TABLE scheduled_maintenance_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    scheduled_maintenance_id UUID NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    previous_planned_date DATE,
    new_planned_date DATE,
    previous_planned_time TIME,
    new_planned_time TIME,
    reason TEXT,
    changed_by UUID NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_scheduled_maintenance_history_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_scheduled_maintenance_history_schedule
        FOREIGN KEY (scheduled_maintenance_id)
        REFERENCES scheduled_maintenance(id),

    CONSTRAINT fk_scheduled_maintenance_history_changed_by
        FOREIGN KEY (changed_by)
        REFERENCES users(id),

    CONSTRAINT chk_scheduled_maintenance_history_previous_status
        CHECK (
            previous_status IS NULL
            OR previous_status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED', 'DELETED')
        ),

    CONSTRAINT chk_scheduled_maintenance_history_new_status
        CHECK (new_status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED', 'DELETED'))
);

CREATE INDEX idx_scheduled_maintenance_history_schedule
    ON scheduled_maintenance_history(scheduled_maintenance_id);

CREATE INDEX idx_scheduled_maintenance_history_org
    ON scheduled_maintenance_history(organization_id);

CREATE INDEX idx_scheduled_maintenance_history_changed_at
    ON scheduled_maintenance_history(changed_at);
