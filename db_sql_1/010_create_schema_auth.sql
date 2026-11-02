-- 010_create_schema_auth.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

CREATE SCHEMA auth;
CREATE TABLE auth.users
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id   VARCHAR(255),
    login         CITEXT NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    email         CITEXT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE auth.roles (
                                          id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

CREATE TABLE auth.user_roles (
                                               user_id     UUID NOT NULL REFERENCES auth.users(id),
    role_id     UUID NOT NULL REFERENCES auth.roles(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES auth.users(id),
    PRIMARY KEY (user_id, role_id)
    );

CREATE TABLE auth.permissions (
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

CREATE INDEX ix_permissions_resource ON auth.permissions (resource, action);
CREATE TABLE auth.role_permissions (
                                                     role_id        UUID NOT NULL REFERENCES auth.roles(id)       ON DELETE CASCADE,
    permission_id  UUID NOT NULL REFERENCES auth.permissions(id) ON DELETE CASCADE,
    granted_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by     UUID REFERENCES auth.users(id),
    PRIMARY KEY (role_id, permission_id)
    );

CREATE INDEX ix_role_permissions_permission ON auth.role_permissions (permission_id);

CREATE VIEW auth.effective_user_permissions AS
SELECT DISTINCT
    ur.user_id,
    p.id   AS permission_id,
    p.code AS permission_code
FROM auth.user_roles ur
         JOIN auth.role_permissions rp ON rp.role_id = ur.role_id
         JOIN auth.permissions p       ON p.id       = rp.permission_id;

CREATE FUNCTION auth.user_has_permission(
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
COMMENT ON SCHEMA auth IS 'Аутентификация и авторизация: пользователи, роли, назначения ролей.';

COMMENT ON TABLE  auth.users IS
    'Пользователи системы. Пароль — только хеш. '
    'System user (id=...0001) — исполнитель автоматических действий, пароль заблокирован (''!'').';
COMMENT ON COLUMN auth.users.id            IS 'Внутренний идентификатор пользователя (UUID)';
COMMENT ON COLUMN auth.users.external_id   IS 'Идентификатор во внешней системе (SSO/LDAP), nullable';
COMMENT ON COLUMN auth.users.login         IS 'Логин (CITEXT, регистронезависимый, уникальный). __system__ — резерв.';
COMMENT ON COLUMN auth.users.password_hash IS 'Хеш пароля (bcrypt/argon2); ''!'' = locked (нет валидного пароля)';
COMMENT ON COLUMN auth.users.display_name  IS 'Отображаемое имя';
COMMENT ON COLUMN auth.users.email         IS 'Email (CITEXT, регистронезависимый), nullable';
COMMENT ON COLUMN auth.users.is_active     IS 'Признак активности. FALSE — вход запрещён, запись сохраняется';
COMMENT ON COLUMN auth.users.created_at    IS 'Момент создания';
COMMENT ON COLUMN auth.users.updated_at    IS 'Момент последнего изменения';

COMMENT ON TABLE  auth.roles IS 'Роли (RBAC). Справочник кодов ролей.';
COMMENT ON COLUMN auth.roles.code        IS 'Уникальный код роли (ADMIN, OPERATOR, ...)';
COMMENT ON COLUMN auth.roles.name        IS 'Человекочитаемое имя роли';
COMMENT ON COLUMN auth.roles.description IS 'Описание роли';
COMMENT ON COLUMN auth.roles.created_at  IS 'Момент создания роли';

COMMENT ON TABLE  auth.user_roles IS 'Связка пользователь ↔ роль (M:N).';
COMMENT ON COLUMN auth.user_roles.user_id     IS 'Пользователь';
COMMENT ON COLUMN auth.user_roles.role_id     IS 'Роль';
COMMENT ON COLUMN auth.user_roles.assigned_at IS 'Момент назначения роли';
COMMENT ON COLUMN auth.user_roles.assigned_by IS 'Кто назначил (nullable — системное назначение)';

COMMIT;
