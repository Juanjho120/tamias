CREATE TABLE maintenance_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_maintenance_categories_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_maintenance_categories_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_maintenance_categories_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE maintenance_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    maintenance_category_id UUID NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_maintenance_types_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_maintenance_types_category
        FOREIGN KEY (maintenance_category_id)
        REFERENCES maintenance_categories(id),

    CONSTRAINT uk_maintenance_types_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_maintenance_types_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE maintenance_people (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(150),
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_maintenance_people_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_maintenance_people_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_maintenance_people_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE platforms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_platforms_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_platforms_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_platforms_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(150),
    address TEXT,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_suppliers_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_suppliers_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_suppliers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE cities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    department VARCHAR(150),
    country VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_cities_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_cities_organization_name_department_country
        UNIQUE (organization_id, name, department, country),

    CONSTRAINT chk_cities_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_materials_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_materials_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_materials_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE brands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_brands_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_brands_organization_name
        UNIQUE (organization_id, name),

    CONSTRAINT chk_brands_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE task_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_task_templates_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_task_templates_organization_title
        UNIQUE (organization_id, title),

    CONSTRAINT chk_task_templates_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_maintenance_categories_organization_status
    ON maintenance_categories(organization_id, status);

CREATE INDEX idx_maintenance_types_organization_status
    ON maintenance_types(organization_id, status);

CREATE INDEX idx_maintenance_types_category
    ON maintenance_types(maintenance_category_id);

CREATE INDEX idx_maintenance_people_organization_status
    ON maintenance_people(organization_id, status);

CREATE INDEX idx_platforms_organization_status
    ON platforms(organization_id, status);

CREATE INDEX idx_suppliers_organization_status
    ON suppliers(organization_id, status);

CREATE INDEX idx_cities_organization_status
    ON cities(organization_id, status);

CREATE INDEX idx_materials_organization_status
    ON materials(organization_id, status);

CREATE INDEX idx_brands_organization_status
    ON brands(organization_id, status);

CREATE INDEX idx_task_templates_organization_status
    ON task_templates(organization_id, status);

CREATE TRIGGER trg_maintenance_categories_set_updated_at
BEFORE UPDATE ON maintenance_categories
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_maintenance_types_set_updated_at
BEFORE UPDATE ON maintenance_types
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_maintenance_people_set_updated_at
BEFORE UPDATE ON maintenance_people
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_platforms_set_updated_at
BEFORE UPDATE ON platforms
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_suppliers_set_updated_at
BEFORE UPDATE ON suppliers
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_cities_set_updated_at
BEFORE UPDATE ON cities
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_materials_set_updated_at
BEFORE UPDATE ON materials
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_brands_set_updated_at
BEFORE UPDATE ON brands
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_task_templates_set_updated_at
BEFORE UPDATE ON task_templates
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
