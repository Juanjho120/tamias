ALTER TABLE maintenance_categories
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE maintenance_types
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE platforms
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE brands
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE materials
ADD COLUMN IF NOT EXISTS unit VARCHAR(50),
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE maintenance_people
ADD COLUMN IF NOT EXISTS full_name VARCHAR(150),
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

UPDATE maintenance_people
SET full_name = name
WHERE full_name IS NULL
  AND EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'maintenance_people'
        AND column_name = 'name'
  );

ALTER TABLE maintenance_people
ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE maintenance_people
DROP CONSTRAINT IF EXISTS uk_maintenance_people_organization_name;

ALTER TABLE maintenance_people
DROP CONSTRAINT IF EXISTS uk_maintenance_people_org_name;

ALTER TABLE maintenance_people
ADD CONSTRAINT uk_maintenance_people_org_full_name
    UNIQUE (organization_id, full_name);

ALTER TABLE maintenance_people
DROP COLUMN IF EXISTS name;

ALTER TABLE suppliers
ADD COLUMN IF NOT EXISTS website TEXT,
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE suppliers
DROP COLUMN IF EXISTS address;

ALTER TABLE cities
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

UPDATE cities
SET country = 'Guatemala'
WHERE country IS NULL;

ALTER TABLE cities
ALTER COLUMN country SET DEFAULT 'Guatemala';

ALTER TABLE cities
DROP CONSTRAINT IF EXISTS uk_cities_organization_name_department_country;

ALTER TABLE cities
DROP CONSTRAINT IF EXISTS uk_cities_org_name;

ALTER TABLE cities
ADD CONSTRAINT uk_cities_org_name
    UNIQUE (organization_id, name);

ALTER TABLE cities
DROP COLUMN IF EXISTS department;

ALTER TABLE cities
DROP COLUMN IF EXISTS description;

ALTER TABLE task_templates
ADD COLUMN IF NOT EXISTS name VARCHAR(150),
ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

UPDATE task_templates
SET name = title
WHERE name IS NULL
  AND EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'task_templates'
        AND column_name = 'title'
  );

ALTER TABLE task_templates
ALTER COLUMN name SET NOT NULL;

ALTER TABLE task_templates
DROP CONSTRAINT IF EXISTS uk_task_templates_organization_title;

ALTER TABLE task_templates
DROP CONSTRAINT IF EXISTS uk_task_templates_org_name;

ALTER TABLE task_templates
ADD CONSTRAINT uk_task_templates_org_name
    UNIQUE (organization_id, name);

ALTER TABLE task_templates
DROP COLUMN IF EXISTS title;
