-- 081_seed_system_user.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

INSERT INTO auth.users (id, login, password_hash, display_name, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', '__system__', '!', 'System', true);
INSERT INTO auth.user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000001'::uuid, id
FROM auth.roles WHERE code = 'INTEGRATION';

COMMIT;
