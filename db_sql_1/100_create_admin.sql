-- 100_create_admin.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

DO $admin$
DECLARE
    admin_login text := 'CHANGE_ME_ADMIN_LOGIN';
    admin_name text := 'CHANGE_ME_ADMIN_NAME';
    admin_password text := 'CHANGE_ME_ADMIN_PASSWORD';
    new_id uuid;
    superuser_id uuid;
BEGIN
    IF admin_login LIKE 'CHANGE_ME%' OR admin_name LIKE 'CHANGE_ME%'
       OR admin_password LIKE 'CHANGE_ME%' OR length(admin_password) < 16
       OR btrim(admin_login) = '' OR btrim(admin_name) = ''
       OR lower(admin_login) = '__system__' THEN
        RAISE EXCEPTION 'Заполните параметры администратора; пароль — не менее 16 символов';
    END IF;
    IF octet_length(admin_password) > 72 THEN
        RAISE EXCEPTION 'Пароль bcrypt не должен превышать 72 байта';
    END IF;
    SELECT id INTO STRICT superuser_id FROM auth.roles WHERE code = 'SUPERUSER';
    INSERT INTO auth.users(login, password_hash, display_name, is_active)
    VALUES (admin_login, public.crypt(admin_password, public.gen_salt('bf', 12)), admin_name, true)
    RETURNING id INTO new_id;
    INSERT INTO auth.user_roles(user_id, role_id) VALUES (new_id, superuser_id);
END $admin$;

COMMIT;
