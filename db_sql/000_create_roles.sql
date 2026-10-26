-- 000_create_roles.sql
-- Выполняется суперпользователем PostgreSQL.
-- Пароль замените на секрет из защищенного хранилища.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'pulse_platform_app'
    ) THEN
        CREATE ROLE pulse_platform_app LOGIN PASSWORD 'password@pulse.ru';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE rolname = 'pulse_platform_readonly'
    ) THEN
        CREATE ROLE pulse_platform_readonly LOGIN PASSWORD 'password@pulse.ru';
    END IF;
END;
$$;

ALTER ROLE pulse_platform_app SET TimeZone TO 'UTC';
ALTER ROLE pulse_platform_readonly SET TimeZone TO 'UTC';
