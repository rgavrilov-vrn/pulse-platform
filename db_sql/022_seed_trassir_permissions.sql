-- TRASSIR ingestion permission and dedicated agent role.

BEGIN;

INSERT INTO auth.permissions (
    code,
    resource,
    action,
    name,
    description
)
VALUES (
    'trassir:ingest',
    'trassir',
    'ingest',
    'Ingest TRASSIR telemetry',
    'Приём конвертов trassir.telemetry через POST /api/v1/trassir/ingest'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.roles (
    code,
    name,
    description
)
VALUES (
    'TRASSIR_AGENT',
    'TRASSIR telemetry agent',
    'Отправка телеметрии TRASSIR без прав управления платформой'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permissions (
    role_id,
    permission_id
)
SELECT role.id, permission.id
FROM auth.roles AS role
CROSS JOIN auth.permissions AS permission
WHERE role.code = 'TRASSIR_AGENT'
  AND permission.code = 'trassir:ingest'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
