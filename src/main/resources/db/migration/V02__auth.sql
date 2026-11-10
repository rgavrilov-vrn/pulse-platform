-- V02__auth.sql
-- Пользователи, роли, назначения ролей. Без RBAC-каталога (см. V03).

SET search_path = pg_catalog, public;

CREATE TABLE auth.users (
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
                                 user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
                                 role_id     UUID NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
                                 assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 assigned_by UUID REFERENCES auth.users(id),
                                 PRIMARY KEY (user_id, role_id)
);

CREATE INDEX ix_user_roles_role ON auth.user_roles (role_id);

-- Триггер updated_at для auth.users
CREATE OR REPLACE FUNCTION public.tg_set_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at := now(); RETURN NEW; END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();

COMMENT ON TABLE  auth.users IS
    'Пользователи. System user (id=…0001) — атрибуция автоматических действий, пароль заблокирован (''!'').';
COMMENT ON COLUMN auth.users.login         IS 'CITEXT: регистронезависимый. __system__ — резерв.';
COMMENT ON COLUMN auth.users.password_hash IS 'bcrypt/argon2. ''!'' = locked (matches() бросит IAE — фильтровать в AppUserDetailsService).';
COMMENT ON TABLE  auth.roles               IS 'Роли (RBAC-коды).';
COMMENT ON TABLE  auth.user_roles          IS 'Пользователь ↔ роль (M:N).';