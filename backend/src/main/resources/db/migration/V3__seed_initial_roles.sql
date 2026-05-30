INSERT INTO roles (code, name, description)
VALUES
    ('ADMINISTRATOR', 'Administrator', 'Full access within the organization'),
    ('PROPERTY_MANAGER', 'Property Manager', 'Manages daily property operations'),
    ('MAINTENANCE_STAFF', 'Maintenance Staff', 'Handles assigned maintenance and tasks'),
    ('READ_ONLY', 'Read Only', 'Can view information but cannot modify it')
ON CONFLICT (code) DO NOTHING;
