-- Grant TRASSIR ingestion permission to ADMIN and SUPERUSER roles.

BEGIN;

INSERT INTO auth.role_permissions (
    role_id,
    permission_id
)
SELECT role.id, permission.id
FROM auth.roles AS role
CROSS JOIN auth.permissions AS permission
WHERE role.code IN ('ADMIN', 'SUPERUSER')
  AND permission.code = 'trassir:ingest'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
