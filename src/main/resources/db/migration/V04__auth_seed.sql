-- V04__auth_seed.sql
-- Seed каталога пермишенов, ролей, дефолтных пользователей, user_roles.
-- DEV-ONLY: password = login для дефолтных пользователей (кроме __system__).

SET search_path = pg_catalog, public;

-- ---------- 1. Пермишены ----------
INSERT INTO auth.permissions (code, resource, action, name, description) VALUES
                                                                             ('incident:read',           'incident', 'read',           'Read incidents',              'Просмотр инцидентов, комментариев, материалов, context_refs'),
                                                                             ('incident:write',          'incident', 'write',          'Create/update incidents',     'Создание/редактирование инцидентов, комментариев, материалов, чек-листа'),
                                                                             ('incident:delete',         'incident', 'delete',         'Delete incidents',            'Удаление инцидентов и связанных записей'),
                                                                             ('incident:review',         'incident', 'review',         'Review incidents',            'Confirm / reject / close / resolve'),
                                                                             ('incident:assign',         'incident', 'assign',         'Assign incidents',            'Назначение/снятие исполнителя'),
                                                                             ('incident:export',         'incident', 'export',         'Export incidents',            'Экспорт инцидентов'),
                                                                             ('incident_catalog:read',   'incident_catalog', 'read',   'Read incident catalog',       'Просмотр категорий/чек-листов/матрицы переходов'),
                                                                             ('incident_catalog:manage', 'incident_catalog', 'manage', 'Manage incident catalog',     'Создание/редактирование справочников домена'),
                                                                             ('source:read',             'source', 'read',             'Read sources',                'Просмотр реестра источников'),
                                                                             ('source:manage',           'source', 'manage',           'Manage sources',              'Регистрация/деактивация источников'),
                                                                             ('source_event:read',       'source_event', 'read',       'Read source events',          'Просмотр входящих source-событий'),
                                                                             ('source_event:process',    'source_event', 'process',    'Process source events',       'Retry / DUPLICATE / FAILED'),
                                                                             ('video:read',              'video', 'read',              'Read video',                  'Просмотр серверов/камер/каналов'),
                                                                             ('video:write',             'video', 'write',             'Create/update video',         'Создание/редактирование серверов/камер/каналов'),
                                                                             ('video:delete',            'video', 'delete',            'Delete video',                'Удаление серверов/камер/каналов'),
                                                                             ('video_event:read',        'video_event', 'read',        'Read video events',           'Просмотр сырых и нормализованных видео-событий'),
                                                                             ('video_event:write',       'video_event', 'write',       'Ingest video events',         'Приём и нормализация видео-событий'),
                                                                             ('ref:read',                'ref', 'read',                'Read reference data',         'Просмотр объектов/зон/подразделений/сотрудников'),
                                                                             ('ref:write',               'ref', 'write',               'Create/update reference data','Создание/редактирование справочников'),
                                                                             ('ref:delete',              'ref', 'delete',              'Delete reference data',       'Удаление записей справочников'),
                                                                             ('user:read',               'user', 'read',               'Read users',                  'Просмотр пользователей'),
                                                                             ('user:write',              'user', 'write',              'Create/update users',         'Создание/редактирование пользователей'),
                                                                             ('user:delete',             'user', 'delete',             'Delete users',                'Удаление пользователей'),
                                                                             ('user:assign_role',        'user', 'assign_role',        'Assign roles to users',       'Назначение/отзыв ролей'),
                                                                             ('role:read',               'role', 'read',               'Read roles',                  'Просмотр ролей'),
                                                                             ('role:manage',             'role', 'manage',             'Manage roles',                'Создание/редактирование ролей, привязка пермишенов'),
                                                                             ('permission:read',         'permission', 'read',         'Read permissions',            'Просмотр каталога пермишенов'),
                                                                             ('report:create',           'report', 'create',           'Create reports',              'Запрос на генерацию отчёта'),
                                                                             ('report:read',             'report', 'read',             'Read reports',                'Просмотр и скачивание отчётов'),
                                                                             ('report:delete',           'report', 'delete',           'Delete reports',              'Удаление готовых отчётов'),
                                                                             ('audit:read',              'audit', 'read',              'Read audit log',              'Просмотр журнала аудита'),
                                                                             ('audit:export',            'audit', 'export',            'Export audit log',            'Экспорт журнала аудита'),
                                                                             ('outbox:read',             'outbox', 'read',             'Read outbox',                 'Просмотр outbox-событий'),
                                                                             ('outbox:retry',            'outbox', 'retry',            'Retry outbox events',         'Ретрай FAILED/DEAD'),
                                                                             ('goodwan:read',            'goodwan', 'read',            'Read goodwan history',        'Просмотр сырой истории goodwan'),
                                                                             ('trassir:ingest',          'trassir', 'ingest',          'Ingest TRASSIR telemetry',    'POST /api/v1/trassir/ingest'),
                                                                             ('trassir:admin',           'trassir', 'admin',           'Administer TRASSIR data',     'Полная очистка и административные операции'),
                                                                             ('system:admin',            'system', 'admin',            'System administrator',        'Полный административный доступ (SUPERUSER-only)')
    ON CONFLICT (code) DO NOTHING;

-- ---------- 2. Роли ----------
INSERT INTO auth.roles (code, name, description) VALUES
                                                     ('SUPERUSER',   'Superuser',     'Unrestricted access, bypasses all checks'),
                                                     ('ADMIN',       'Administrator', 'Full administrative access except system:admin'),
                                                     ('OPERATOR',    'Operator',      'Creates and works on incidents'),
                                                     ('REVIEWER',    'Reviewer',      'Reviews, confirms, rejects and closes incidents'),
                                                     ('VIEWER',      'Viewer',        'Read-only access'),
                                                     ('INTEGRATION', 'Integration',   'Service account for integrations'),
                                                     ('TRASSIR_AGENT','TRASSIR telemetry agent','Отправка телеметрии TRASSIR')
    ON CONFLICT (code) DO NOTHING;

-- ---------- 3. role_permissions ----------
-- SUPERUSER — все пермишены, включая system:admin и trassir:admin
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth.roles r CROSS JOIN auth.permissions p
WHERE r.code = 'SUPERUSER'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMIN — все, кроме system:admin
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth.roles r JOIN auth.permissions p ON p.code <> 'system:admin'
WHERE r.code = 'ADMIN'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- OPERATOR
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth.roles r JOIN auth.permissions p ON p.code IN (
                                                                          'incident:read','incident:write','incident:assign','incident:export',
                                                                          'incident_catalog:read','source:read','source_event:read',
                                                                          'video:read','video_event:read','ref:read','report:create','report:read'
    ) WHERE r.code = 'OPERATOR'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- REVIEWER
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth.roles r JOIN auth.permissions p ON p.code IN (
                                                                          'incident:read','incident:review','incident:assign','incident:export',
                                                                          'incident_catalog:read','source:read','source_event:read',
                                                                          'video:read','video_event:read','ref:read','report:read','audit:read'
    ) WHERE r.code = 'REVIEWER'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- VIEWER
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth.roles r JOIN auth.permissions p ON p.code IN (
                                                                          'incident:read','incident_catalog:read','source:read',
                                                                          'video:read','video_event:read','ref:read','report:read'
    ) WHERE r.code = 'VIEWER'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- INTEGRATION
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth.roles r JOIN auth.permissions p ON p.code IN (
                                                                          'incident:read','incident:write','incident_catalog:read',
                                                                          'source:read','source:manage','source_event:read','source_event:process',
                                                                          'video:read','video:write','video_event:read','video_event:write',
                                                                          'ref:read','outbox:read'
    ) WHERE r.code = 'INTEGRATION'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TRASSIR_AGENT
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM auth.roles r JOIN auth.permissions p ON p.code = 'trassir:ingest'
WHERE r.code = 'TRASSIR_AGENT'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------- 4. Пользователи (DEV-ONLY: password = login) ----------
WITH new_users(login, display_name, email) AS (
    VALUES
        ('admin',       'Administrator',        'admin@example.com'),
        ('superuser',   'Superuser',            'superuser@example.com'),
        ('operator',    'Operator',             'operator@example.com'),
        ('reviewer',    'Reviewer',             'reviewer@example.com'),
        ('viewer',      'Viewer',               'viewer@example.com'),
        ('integration', 'Integration Service',  'integration@example.com')
)
INSERT INTO auth.users (login, password_hash, display_name, email, is_active)
SELECT login, crypt(login, gen_salt('bf', 12)), display_name, email, TRUE
FROM new_users
    ON CONFLICT (login) DO NOTHING;

-- System user (locked: '!' не является валидным bcrypt-хешем)
INSERT INTO auth.users (id, login, password_hash, display_name, email, is_active)
VALUES ('00000000-0000-0000-0000-000000000001'::uuid,
        '__system__', '!', 'System', NULL, TRUE)
    ON CONFLICT (id) DO NOTHING;

-- ---------- 5. user_roles ----------
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth.users u JOIN auth.roles r ON r.code IN ('ADMIN','SUPERUSER')
WHERE u.login = 'admin'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth.users u JOIN auth.roles r ON r.code = 'SUPERUSER'
WHERE u.login = 'superuser'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth.users u JOIN auth.roles r ON r.code = 'OPERATOR'
WHERE u.login = 'operator'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth.users u JOIN auth.roles r ON r.code = 'REVIEWER'
WHERE u.login = 'reviewer'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth.users u JOIN auth.roles r ON r.code = 'VIEWER'
WHERE u.login = 'viewer'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth.users u JOIN auth.roles r ON r.code = 'INTEGRATION'
WHERE u.login = 'integration'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth.users u JOIN auth.roles r ON r.code = 'INTEGRATION'
WHERE u.id = '00000000-0000-0000-0000-000000000001'::uuid
ON CONFLICT (user_id, role_id) DO NOTHING;