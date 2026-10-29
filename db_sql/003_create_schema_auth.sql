-- =============================================================================
-- 003_create_schema_auth.sql
-- FIX: заголовок приведён к фактическому номеру файла (было -- 004_...).
-- -----------------------------------------------------------------------------
-- Аутентификация и авторизация (auth schema) + seed-данные:
--   * admin user + role ADMIN;
--   * system user (id=00000000-0000-0000-0000-000000000001).
--
-- FIX: блок «Порядок миграций» приведён в соответствие реальным именам файлов.
-- Порядок исполнения по именам файлов (алфавитный) корректен:
--   001_create_database
--   → 002_init_schemas
--   → 003_create_schema_auth
--   → 004_create_schema_audit   (зависит от auth.users)
--   → 005_create_schema_outbox
--   → 006_create_schema_refs
--   → 007_create_schema_incident (зависит от auth.users + incident.sources seed)
--   → 007_create_schema_videoserver (зависит от ref.*)
--   → 008_create_schema_goodwan
--   → 020_permissions
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS auth;

-- -----------------------------------------------------------------------------
-- Таблицы
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth.users
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

CREATE TABLE IF NOT EXISTS auth.roles (
                                          id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS auth.user_roles (
                                               user_id     UUID NOT NULL REFERENCES auth.users(id),
    role_id     UUID NOT NULL REFERENCES auth.roles(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES auth.users(id),
    PRIMARY KEY (user_id, role_id)
    );

-- -----------------------------------------------------------------------------
-- Seed: admin user
-- -----------------------------------------------------------------------------
INSERT INTO auth.users (login, password_hash, display_name, email, is_active)
VALUES (
           'admin',
           '$2b$12$90RI9oyUR3E4cf8gmNukBe4RJjVOrOH6SzOIlN0ACDetEzbZA2hNW',
           'Administrator',
           'admin@example.com',
           TRUE
       )
    ON CONFLICT (login) DO NOTHING;

INSERT INTO auth.roles (code, name, description)
VALUES ('ADMIN', 'Administrator', 'Full administrative access')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'ADMIN'
WHERE u.login = 'admin'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Seed: SYSTEM user
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM auth.users
        WHERE id = '00000000-0000-0000-0000-000000000001'::uuid
    ) THEN
        INSERT INTO auth.users (
            id, login, password_hash, display_name, email, is_active, created_at, updated_at
        ) VALUES (
            '00000000-0000-0000-0000-000000000001'::uuid,
            '__system__',
            '!',        -- locked: BCryptPasswordEncoder.matches() → false
            'System',
            NULL,
            TRUE,       -- аккаунт «активен» для attribution; войти нельзя
            now(),
            now()
        );
END IF;
END $$;

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
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