CREATE TABLE task_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NOT NULL,
    reservation_id UUID NULL,
    maintenance_record_id UUID NULL,
    title VARCHAR(150) NOT NULL,
    creation_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_task_lists_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_task_lists_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_task_lists_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT fk_task_lists_maintenance_record
        FOREIGN KEY (maintenance_record_id)
        REFERENCES maintenance_records(id),

    CONSTRAINT fk_task_lists_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_task_lists_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),

    CONSTRAINT fk_task_lists_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_task_lists_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'DELETED'))
);

CREATE TABLE task_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    task_list_id UUID NOT NULL,
    task_template_id UUID NULL,
    task_name VARCHAR(150) NOT NULL,
    responsible_person VARCHAR(150),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completion_date TIMESTAMP WITH TIME ZONE NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_task_items_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_task_items_task_list
        FOREIGN KEY (task_list_id)
        REFERENCES task_lists(id),

    CONSTRAINT fk_task_items_task_template
        FOREIGN KEY (task_template_id)
        REFERENCES task_templates(id)
);

CREATE INDEX idx_task_lists_org_property
    ON task_lists(organization_id, property_id);

CREATE INDEX idx_task_lists_due_date
    ON task_lists(organization_id, due_date);

CREATE INDEX idx_task_lists_reservation
    ON task_lists(reservation_id);

CREATE INDEX idx_task_lists_maintenance_record
    ON task_lists(maintenance_record_id);

CREATE INDEX idx_task_lists_org_status
    ON task_lists(organization_id, status);

CREATE INDEX idx_task_items_task_list
    ON task_items(task_list_id);

CREATE INDEX idx_task_items_completed
    ON task_items(organization_id, completed);

CREATE TRIGGER trg_task_lists_set_updated_at
BEFORE UPDATE ON task_lists
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_task_items_set_updated_at
BEFORE UPDATE ON task_items
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
