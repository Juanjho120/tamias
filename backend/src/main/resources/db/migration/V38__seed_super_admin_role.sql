INSERT INTO roles (code, name, description)
VALUES (
    'SUPER_ADMIN',
    'Super Administrator',
    'Platform-level administrator with access to manage all organizations'
)
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;
