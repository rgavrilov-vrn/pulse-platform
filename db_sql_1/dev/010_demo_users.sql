-- dev/010_demo_users.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

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

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'SUPERUSER'
WHERE u.login = 'superuser'
    ON CONFLICT (user_id, role_id) DO NOTHING;
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'OPERATOR'
WHERE u.login = 'operator'
    ON CONFLICT (user_id, role_id) DO NOTHING;
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'REVIEWER'
WHERE u.login = 'reviewer'
    ON CONFLICT (user_id, role_id) DO NOTHING;
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'VIEWER'
WHERE u.login = 'viewer'
    ON CONFLICT (user_id, role_id) DO NOTHING;
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
         JOIN auth.roles r ON r.code = 'INTEGRATION'
WHERE u.login = 'integration'
    ON CONFLICT (user_id, role_id) DO NOTHING;

COMMIT;
