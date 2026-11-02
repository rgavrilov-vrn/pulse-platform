-- 080_seed_roles_and_permissions.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

INSERT INTO auth.permissions (code, resource, action, name, description) VALUES
('incident:read',          'incident', 'read',         'Read incidents',              'Просмотр инцидентов, комментариев, материалов, context_refs'),
('incident:write',         'incident', 'write',        'Create/update incidents',     'Создание/редактирование инцидентов, комментариев, материалов, чек-листа'),
('incident:delete',        'incident', 'delete',       'Delete incidents',            'Удаление инцидентов и связанных записей'),
('incident:review',        'incident', 'review',       'Review incidents',            'Confirm / reject / close / resolve инцидентов'),
('incident:assign',        'incident', 'assign',       'Assign incidents',            'Назначение и снятие исполнителя'),
('incident:export',        'incident', 'export',       'Export incidents',            'Экспорт инцидентов и связанных материалов'),
('incident_catalog:read',  'incident_catalog', 'read',  'Read incident catalog',       'Просмотр категорий нарушений, чек-листов, матрицы переходов'),
('incident_catalog:manage','incident_catalog', 'manage','Manage incident catalog',     'Создание/редактирование справочников домена инцидентов'),
('source:read',            'source', 'read',           'Read sources',                'Просмотр реестра источников'),
('source:manage',          'source', 'manage',         'Manage sources',              'Регистрация/деактивация источников'),
('source_event:read',      'source_event', 'read',     'Read source events',          'Просмотр входящих source-событий'),
('source_event:process',   'source_event', 'process',  'Process source events',       'Повторная обработка / retry / пометка DUPLICATE / FAILED'),
('video:read',             'video', 'read',            'Read video',                  'Просмотр серверов, камер, каналов'),
('video:write',            'video', 'write',           'Create/update video',         'Создание/редактирование серверов, камер, каналов'),
('video:delete',           'video', 'delete',          'Delete video',                'Удаление серверов, камер, каналов'),
('video_event:read',       'video_event', 'read',      'Read video events',           'Просмотр сырых и нормализованных видео-событий'),
('video_event:write',      'video_event', 'write',     'Ingest video events',         'Приём и нормализация входящих видео-событий'),
('ref:read',               'ref', 'read',              'Read reference data',         'Просмотр объектов, зон, подразделений, сотрудников'),
('ref:write',              'ref', 'write',             'Create/update reference data','Создание/редактирование справочников'),
('ref:delete',             'ref', 'delete',            'Delete reference data',       'Удаление записей справочников'),
('user:read',              'user', 'read',             'Read users',                  'Просмотр пользователей'),
('user:write',             'user', 'write',            'Create/update users',         'Создание/редактирование пользователей'),
('user:delete',            'user', 'delete',           'Delete users',                'Удаление пользователей'),
('user:assign_role',       'user', 'assign_role',      'Assign roles to users',       'Назначение/отзыв ролей'),
('role:read',              'role', 'read',             'Read roles',                  'Просмотр ролей и их пермишенов'),
('role:manage',            'role', 'manage',           'Manage roles',                'Создание/редактирование ролей, назначение им пермишенов'),
('permission:read',        'permission', 'read',       'Read permissions',            'Просмотр каталога пермишенов'),
('report:create',          'report', 'create',         'Create reports',              'Запрос на генерацию отчёта'),
('report:read',            'report', 'read',           'Read reports',                'Просмотр и скачивание отчётов'),
('report:delete',          'report', 'delete',         'Delete reports',              'Удаление готовых отчётов'),
('audit:read',             'audit', 'read',            'Read audit log',              'Просмотр журнала аудита'),
('audit:export',           'audit', 'export',          'Export audit log',            'Экспорт журнала аудита'),
('outbox:read',            'outbox', 'read',           'Read outbox',                 'Просмотр outbox-событий и статусов публикации'),
('outbox:retry',           'outbox', 'retry',          'Retry outbox events',         'Ретрай FAILED/DEAD outbox-событий'),
('goodwan:read',           'goodwan', 'read',          'Read goodwan history',        'Просмотр сырой истории goodwan'),
('system:admin',           'system', 'admin',          'System administrator',        'Полный административный доступ (SUPERUSER-only)')
    ON CONFLICT (code) DO NOTHING;
INSERT INTO auth.roles (code, name, description) VALUES
                                                     ('SUPERUSER',   'Superuser',         'Unrestricted access, bypasses all checks (system administrator)'),
                                                     ('ADMIN',       'Administrator',     'Full administrative access except system:admin'),
                                                     ('OPERATOR',    'Operator',          'Creates and works on incidents'),
                                                     ('REVIEWER',    'Reviewer',          'Reviews, confirms, rejects and closes incidents'),
                                                     ('VIEWER',      'Viewer',            'Read-only access to incidents and reference data'),
                                                     ('INTEGRATION', 'Integration',       'Service account for integrations')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         CROSS JOIN auth.permissions p
WHERE r.code = 'SUPERUSER';

INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         JOIN auth.permissions p ON p.code <> 'system:admin'
WHERE r.code = 'ADMIN';
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         JOIN auth.permissions p ON p.code IN (
                                               'incident:read', 'incident:write', 'incident:assign', 'incident:export',
                                               'incident_catalog:read',
                                               'source:read', 'source_event:read',
                                               'video:read', 'video_event:read',
                                               'ref:read',
                                               'report:create', 'report:read'
    )
WHERE r.code = 'OPERATOR'
    ON CONFLICT (role_id, permission_id) DO NOTHING;
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         JOIN auth.permissions p ON p.code IN (
                                               'incident:read', 'incident:review', 'incident:assign', 'incident:export',
                                               'incident_catalog:read',
                                               'source:read', 'source_event:read',
                                               'video:read', 'video_event:read',
                                               'ref:read',
                                               'report:read',
                                               'audit:read'
    )
WHERE r.code = 'REVIEWER'
    ON CONFLICT (role_id, permission_id) DO NOTHING;
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         JOIN auth.permissions p ON p.code IN (
                                               'incident:read',
                                               'incident_catalog:read',
                                               'source:read',
                                               'video:read', 'video_event:read',
                                               'ref:read',
                                               'report:read'
    )
WHERE r.code = 'VIEWER'
    ON CONFLICT (role_id, permission_id) DO NOTHING;
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         JOIN auth.permissions p ON p.code IN (
                                               'incident:read', 'incident:write',
                                               'incident_catalog:read',
                                               'source:read', 'source:manage',
                                               'source_event:read', 'source_event:process',
                                               'video:read', 'video:write',
                                               'video_event:read', 'video_event:write',
                                               'ref:read',
                                               'outbox:read'
    )
WHERE r.code = 'INTEGRATION'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
