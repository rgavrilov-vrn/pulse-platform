-- Выполнить суперпользователем в postgres. Заменить оба пароля локально.
BEGIN;
DO $roles$
DECLARE
    app_password text := 'CHANGE_ME_APP_PASSWORD';
    readonly_password text := 'CHANGE_ME_READONLY_PASSWORD';
BEGIN
    IF app_password LIKE 'CHANGE_ME%' OR readonly_password LIKE 'CHANGE_ME%'
       OR length(app_password) < 16 OR length(readonly_password) < 16
       OR app_password = readonly_password THEN
        RAISE EXCEPTION 'Задайте разные пароли длиной не менее 16 символов';
    END IF;
    CREATE ROLE pulse_platform_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
    EXECUTE format('CREATE ROLE pulse_platform_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', app_password);
    EXECUTE format('CREATE ROLE pulse_platform_readonly LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', readonly_password);
END $roles$;
ALTER ROLE pulse_platform_app SET TimeZone TO 'UTC';
ALTER ROLE pulse_platform_readonly SET TimeZone TO 'UTC';
COMMIT;
