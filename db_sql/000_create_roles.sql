-- 000_create_roles.sql
-- Выполняется суперпользователем PostgreSQL.
--
-- ВНИМАНИЕ: пароли ниже — DEV-ONLY. В production роли создаются без пароля,
-- а пароль задаётся отдельным шагом из секрет-хранилища:
--     ALTER ROLE pulse_platform_app       PASSWORD '<from-vault>';
--     ALTER ROLE pulse_platform_readonly  PASSWORD '<from-vault>';
--
-- search_path для ролей приложения фиксируется отдельно (см. примечание
-- в конце 020_permissions.sql): приложение не должно зависеть от дефолта.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pulse_platform_app') THEN
CREATE ROLE pulse_platform_app LOGIN PASSWORD 'password@pulse.ru';
END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pulse_platform_readonly') THEN
CREATE ROLE pulse_platform_readonly LOGIN PASSWORD 'password@pulse.ru';
END IF;
END;
$$;

ALTER ROLE pulse_platform_app       SET TimeZone TO 'UTC';
ALTER ROLE pulse_platform_readonly  SET TimeZone TO 'UTC';