CREATE TABLE maintenance_record_people (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    maintenance_record_id UUID NOT NULL,
    maintenance_person_id UUID NOT NULL,

    CONSTRAINT fk_maintenance_record_people_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_maintenance_record_people_record
        FOREIGN KEY (maintenance_record_id)
        REFERENCES maintenance_records(id),

    CONSTRAINT fk_maintenance_record_people_person
        FOREIGN KEY (maintenance_person_id)
        REFERENCES maintenance_people(id),

    CONSTRAINT uk_maintenance_record_people
        UNIQUE (maintenance_record_id, maintenance_person_id)
);

CREATE TABLE maintenance_materials_used (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    maintenance_record_id UUID NOT NULL,
    material_id UUID NULL,
    material_name_snapshot VARCHAR(150) NOT NULL,
    quantity NUMERIC(12, 2),
    unit VARCHAR(50),
    notes TEXT,

    CONSTRAINT fk_maintenance_materials_used_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_maintenance_materials_used_record
        FOREIGN KEY (maintenance_record_id)
        REFERENCES maintenance_records(id),

    CONSTRAINT fk_maintenance_materials_used_material
        FOREIGN KEY (material_id)
        REFERENCES materials(id),

    CONSTRAINT chk_maintenance_materials_used_quantity
        CHECK (quantity IS NULL OR quantity > 0)
);

CREATE INDEX idx_maintenance_record_people_record
    ON maintenance_record_people(maintenance_record_id);

CREATE INDEX idx_maintenance_record_people_person
    ON maintenance_record_people(maintenance_person_id);

CREATE INDEX idx_maintenance_record_people_organization
    ON maintenance_record_people(organization_id);

CREATE INDEX idx_maintenance_materials_record
    ON maintenance_materials_used(maintenance_record_id);

CREATE INDEX idx_maintenance_materials_material
    ON maintenance_materials_used(material_id);

CREATE INDEX idx_maintenance_materials_organization
    ON maintenance_materials_used(organization_id);
