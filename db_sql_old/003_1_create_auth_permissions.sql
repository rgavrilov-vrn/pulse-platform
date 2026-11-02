-- =============================================================================
-- 003_1_create_auth_permissions.sql
-- -----------------------------------------------------------------------------
-- RBAC: каталог пермишенов + назначение на роли + seed-пользователей.
--
-- Модель:
--   auth.users ─< auth.user_roles >─ auth.roles ─< auth.role_permissions >─ auth.permissions
--
-- Права пользователя = union пермишенов всех его ролей
-- (см. view auth.effective_user_permissions, function auth.user_has_permission).
--
-- Прямых grant'ов на пользователя нет: auth.user_permissions удалена
-- (использовалась в ранней версии) — исключения делаются через роли.
--
-- Зависит от:
--   002_init_schemas.sql        (pgcrypto: crypt / gen_salt)
--   003_create_schema_auth.sql  (auth.users / roles / user_roles + seed admin)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Очистка устаревшей auth.user_permissions (удалена из модели).
--    Guarded DROP: на чистой БД таблицы нет — не пишем NOTICE.
--    CASCADE снесёт старый view effective_user_permissions, если он на неё
--    ссылался; пересоздадим ниже через CREATE OR REPLACE.
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_tables
        WHERE schemaname = 'auth'
          AND tablename  = 'user_permissions'
    ) THEN
DROP TABLE auth.user_permissions CASCADE;
END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1. Каталог пермишенов
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.permissions (
                                                id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    resource    VARCHAR(50)  NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_permissions_code_format  CHECK (code     ~ '^[a-z][a-z0-9_]*:[a-z][a-z0-9_]*$'),
    CONSTRAINT chk_permissions_code_matches CHECK (code     = resource || ':' || action),
    CONSTRAINT chk_permissions_resource     CHECK (resource ~ '^[a-z][a-z0-9_]*$'),
    CONSTRAINT chk_permissions_action       CHECK (action   ~ '^[a-z][a-z0-9_]*$')
    );

CREATE INDEX IF NOT EXISTS ix_permissions_resource ON auth.permissions (resource, action);

-- -----------------------------------------------------------------------------
-- 2. Назначение пермишенов на роли
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.role_permissions (
                                                     role_id        UUID NOT NULL REFERENCES auth.roles(id)       ON DELETE CASCADE,
    permission_id  UUID NOT NULL REFERENCES auth.permissions(id) ON DELETE CASCADE,
    granted_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by     UUID REFERENCES auth.users(id),
    PRIMARY KEY (role_id, permission_id)
    );

CREATE INDEX IF NOT EXISTS ix_role_permissions_permission ON auth.role_permissions (permission_id);

-- =============================================================================
-- 3. SEED: каталог пермишенов (по сущностям)
-- =============================================================================
INSERT INTO auth.permissions (code, resource, action, name, description) VALUES
-- incidents
('incident:read',          'incident', 'read',         'Read incidents',              'Просмотр инцидентов, комментариев, материалов, context_refs'),
('incident:write',         'incident', 'write',        'Create/update incidents',     'Создание/редактирование инцидентов, комментариев, материалов, чек-листа'),
('incident:delete',        'incident', 'delete',       'Delete incidents',            'Удаление инцидентов и связанных записей'),
('incident:review',        'incident', 'review',       'Review incidents',            'Confirm / reject / close / resolve инцидентов'),
('incident:assign',        'incident', 'assign',       'Assign incidents',            'Назначение и снятие исполнителя'),
('incident:export',        'incident', 'export',       'Export incidents',            'Экспорт инцидентов и связанных материалов'),

-- incident catalog (violation_categories, checklist_items, status_transitions)
('incident_catalog:read',  'incident_catalog', 'read',  'Read incident catalog',       'Просмотр категорий нарушений, чек-листов, матрицы переходов'),
('incident_catalog:manage','incident_catalog', 'manage','Manage incident catalog',     'Создание/редактирование справочников домена инцидентов'),

-- sources (incident.sources)
('source:read',            'source', 'read',           'Read sources',                'Просмотр реестра источников'),
('source:manage',          'source', 'manage',         'Manage sources',              'Регистрация/деактивация источников'),

-- source_events (incident.source_events)
('source_event:read',      'source_event', 'read',     'Read source events',          'Просмотр входящих source-событий'),
('source_event:process',   'source_event', 'process',  'Process source events',       'Повторная обработка / retry / пометка DUPLICATE / FAILED'),

-- video (servers, cameras, channels)
('video:read',             'video', 'read',            'Read video',                  'Просмотр серверов, камер, каналов'),
('video:write',            'video', 'write',           'Create/update video',         'Создание/редактирование серверов, камер, каналов'),
('video:delete',           'video', 'delete',          'Delete video',                'Удаление серверов, камер, каналов'),

-- video_event (raw + normalized)
('video_event:read',       'video_event', 'read',      'Read video events',           'Просмотр сырых и нормализованных видео-событий'),
('video_event:write',      'video_event', 'write',     'Ingest video events',         'Приём и нормализация входящих видео-событий'),

-- ref (objects, zones, departments, employees)
('ref:read',               'ref', 'read',              'Read reference data',         'Просмотр объектов, зон, подразделений, сотрудников'),
('ref:write',              'ref', 'write',             'Create/update reference data','Создание/редактирование справочников'),
('ref:delete',             'ref', 'delete',            'Delete reference data',       'Удаление записей справочников'),

-- users
('user:read',              'user', 'read',             'Read users',                  'Просмотр пользователей'),
('user:write',             'user', 'write',            'Create/update users',         'Создание/редактирование пользователей'),
('user:delete',            'user', 'delete',           'Delete users',                'Удаление пользователей'),
('user:assign_role',       'user', 'assign_role',      'Assign roles to users',       'Назначение/отзыв ролей'),

-- roles / permissions
('role:read',              'role', 'read',             'Read roles',                  'Просмотр ролей и их пермишенов'),
('role:manage',            'role', 'manage',           'Manage roles',                'Создание/редактирование ролей, назначение им пермишенов'),
('permission:read',        'permission', 'read',       'Read permissions',            'Просмотр каталога пермишенов'),

-- reports (incident.report_jobs)
('report:create',          'report', 'create',         'Create reports',              'Запрос на генерацию отчёта'),
('report:read',            'report', 'read',           'Read reports',                'Просмотр и скачивание отчётов'),
('report:delete',          'report', 'delete',         'Delete reports',              'Удаление готовых отчётов'),

-- audit
('audit:read',             'audit', 'read',            'Read audit log',              'Просмотр журнала аудита'),
('audit:export',           'audit', 'export',          'Export audit log',            'Экспорт журнала аудита'),

-- outbox
('outbox:read',            'outbox', 'read',           'Read outbox',                 'Просмотр outbox-событий и статусов публикации'),
('outbox:retry',           'outbox', 'retry',          'Retry outbox events',         'Ретрай FAILED/DEAD outbox-событий'),

-- goodwan
('goodwan:read',           'goodwan', 'read',          'Read goodwan history',        'Просмотр сырой истории goodwan'),

-- system
('system:admin',           'system', 'admin',          'System administrator',        'Полный административный доступ (SUPERUSER-only)')
    ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- 4. SEED: роли
-- =============================================================================
INSERT INTO auth.roles (code, name, description) VALUES
                                                     ('SUPERUSER',   'Superuser',         'Unrestricted access, bypasses all checks (system administrator)'),
                                                     ('ADMIN',       'Administrator',     'Full administrative access except system:admin'),
                                                     ('OPERATOR',    'Operator',          'Creates and works on incidents'),
                                                     ('REVIEWER',    'Reviewer',          'Reviews, confirms, rejects and closes incidents'),
                                                     ('VIEWER',      'Viewer',            'Read-only access to incidents and reference data'),
                                                     ('INTEGRATION', 'Integration',       'Service account for integrations')
    ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- 5. SEED: пользователи (login = password)
-- =============================================================================
-- 5.1 admin: сброс пароля с исходного hardcoded hash на 'admin'/'admin'.
--     Идемпотентно: обновляем только если хеш всё ещё исходный (из 003).
UPDATE auth.users
SET password_hash = crypt('admin', gen_salt('bf', 12)),
    updated_at    = now()
WHERE login = 'admin'
  AND password_hash = '$2b$12$90RI9oyUR3E4cf8gmNukBe4RJjVOrOH6SzOIlN0ACDetEzbZA2hNW';

-- 5.2 остальные пользователи: login = password
WITH new_users(login, display_name, email) AS (
    VALUES
        ('superuser',   'Superuser',             'superuser@example.com'),
        ('operator',    'Operator',              'operator@example.com'),
        ('reviewer',    'Reviewer',              'reviewer@example.com'),
        ('viewer',      'Viewer',                'viewer@example.com'),
        ('integration', 'Integration Service',   'integration@example.com')
)
INSERT INTO auth.users (login, password_hash, display_name, email, is_active)
SELECT login,
       crypt(login, gen_salt('bf', 12)),   -- password = login
       display_name,
       email,
       TRUE
FROM new_users
    ON CONFLICT (login) DO NOTHING;

-- =============================================================================
-- 6. SEED: role_permissions
-- =============================================================================
-- 6.1 SUPERUSER — всегда ВСЕ пермишены (детерминированно: DELETE + INSERT).
--     Гарантирует, что при добавлении новых пермишенов в каталог
--     SUPERUSER их автоматически получает.
DELETE FROM auth.role_permissions
WHERE role_id = (SELECT id FROM auth.roles WHERE code = 'SUPERUSER');

INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         CROSS JOIN auth.permissions p
WHERE r.code = 'SUPERUSER';

-- 6.2 ADMIN — всё, кроме system:admin.
DELETE FROM auth.role_permissions
WHERE role_id = (SELECT id FROM auth.roles WHERE code = 'ADMIN');

INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         JOIN auth.permissions p ON p.code <> 'system:admin'
WHERE r.code = 'ADMIN';

-- 6.3 OPERATOR
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

-- 6.4 REVIEWER
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

-- 6.5 VIEWER
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

-- 6.6 INTEGRATION
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

-- =============================================================================
-- 7. SEED: user_roles
-- =============================================================================
-- 7.1 admin → ADMIN + SUPERUSER
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code IN ('ADMIN', 'SUPERUSER')
WHERE u.login = 'admin'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- 7.2 superuser → SUPERUSER
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'SUPERUSER'
WHERE u.login = 'superuser'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- 7.3 operator → OPERATOR
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'OPERATOR'
WHERE u.login = 'operator'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- 7.4 reviewer → REVIEWER
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'REVIEWER'
WHERE u.login = 'reviewer'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- 7.5 viewer → VIEWER
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'VIEWER'
WHERE u.login = 'viewer'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- 7.6 integration → INTEGRATION
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'INTEGRATION'
WHERE u.login = 'integration'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- 7.7 __system__ → INTEGRATION
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'INTEGRATION'
WHERE u.id = '00000000-0000-0000-0000-000000000001'::uuid
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =============================================================================
-- 8. Эффективные пермишены пользователя (только через роли)
-- =============================================================================
CREATE OR REPLACE VIEW auth.effective_user_permissions AS
SELECT DISTINCT
    ur.user_id,
    p.id   AS permission_id,
    p.code AS permission_code
FROM auth.user_roles ur
         JOIN auth.role_permissions rp ON rp.role_id = ur.role_id
         JOIN auth.permissions p       ON p.id       = rp.permission_id;

CREATE OR REPLACE FUNCTION auth.user_has_permission(
    p_user_id UUID,
    p_permission_code VARCHAR
) RETURNS BOOLEAN
    LANGUAGE sql STABLE AS $$
SELECT EXISTS (
    SELECT 1
    FROM auth.effective_user_permissions eup
    WHERE eup.user_id = p_user_id
      AND eup.permission_code = p_permission_code
);
$$;

-- =============================================================================
-- 9. Комментарии
-- =============================================================================
COMMENT ON TABLE  auth.permissions IS 'Каталог пермишенов. Code = resource:action (CHECK гарантирует согласованность).';
COMMENT ON COLUMN auth.permissions.code     IS 'Уникальный код пермишена: resource:action';
COMMENT ON COLUMN auth.permissions.resource IS 'Ресурс: incident, incident_catalog, source, source_event, video, video_event, ref, user, role, permission, report, audit, outbox, goodwan, system';
COMMENT ON COLUMN auth.permissions.action   IS 'Действие: read, write, delete, review, assign, export, manage, process, retry, create, admin, assign_role';

COMMENT ON TABLE  auth.role_permissions IS 'Назначение пермишенов на роли (M:N).';
COMMENT ON COLUMN auth.role_permissions.granted_at IS 'Момент выдачи';
COMMENT ON COLUMN auth.role_permissions.granted_by IS 'Кто выдал (nullable — системное назначение)';

COMMENT ON VIEW auth.effective_user_permissions IS
    'Итоговые пермишены пользователя как union всех его ролей.';

COMMENT ON FUNCTION auth.user_has_permission(UUID, VARCHAR) IS
    'TRUE, если у пользователя есть указанный пермишен (через union ролей).';