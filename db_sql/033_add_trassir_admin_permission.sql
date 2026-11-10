BEGIN;

INSERT INTO auth.permissions (
    code,
    resource,
    action,
    name,
    description
)
VALUES (
    'trassir:admin',
    'trassir',
    'admin',
    'Administer TRASSIR data',
    'Полная очистка и административные операции Trassir'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permissions (
    role_id,
    permission_id
)
SELECT role.id, permission.id
FROM auth.roles AS role
CROSS JOIN auth.permissions AS permission
WHERE role.code = 'ADMIN'
  AND permission.code = 'trassir:admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
