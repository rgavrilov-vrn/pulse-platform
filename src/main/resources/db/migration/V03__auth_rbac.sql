-- V03__auth_rbac.sql
-- Каталог пермишенов, привязка к ролям, view/functions для эффективных прав.

SET search_path = pg_catalog, public;

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
                                       role_id       UUID NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
                                       permission_id UUID NOT NULL REFERENCES auth.permissions(id) ON DELETE CASCADE,
                                       granted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       granted_by    UUID REFERENCES auth.users(id),
                                       PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX ix_role_permissions_permission ON auth.role_permissions (permission_id);

CREATE OR REPLACE VIEW auth.effective_user_permissions AS
SELECT DISTINCT ur.user_id, p.id AS permission_id, p.code AS permission_code
FROM auth.user_roles ur
         JOIN auth.role_permissions rp ON rp.role_id = ur.role_id
         JOIN auth.permissions p       ON p.id = rp.permission_id;

CREATE OR REPLACE FUNCTION auth.user_has_permission(p_user_id UUID, p_permission_code VARCHAR)
    RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
SELECT EXISTS (
    SELECT 1 FROM auth.effective_user_permissions eup
    WHERE eup.user_id = p_user_id AND eup.permission_code = p_permission_code
);
$$;

COMMENT ON VIEW auth.effective_user_permissions IS 'Union пермишенов всех ролей пользователя.';