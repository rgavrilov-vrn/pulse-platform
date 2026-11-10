-- 01_create_roles.sql
-- Выполняется суперпользователем PostgreSQL, подключение к системной БД (postgres).
-- DEV-ONLY пароли. В production задаются из vault:
--   ALTER ROLE pulse_platform_app      PASSWORD '<from-vault>';
--   ALTER ROLE pulse_platform_readonly PASSWORD '<from-vault>';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pulse_platform_app') THEN
        EXECUTE 'CREATE ROLE pulse_platform_app LOGIN PASSWORD ''password@pulse.ru''';
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pulse_platform_readonly') THEN
        EXECUTE 'CREATE ROLE pulse_platform_readonly LOGIN PASSWORD ''password@pulse.ru''';
END IF;
END $$;

ALTER ROLE pulse_platform_app      SET TimeZone TO 'UTC';
ALTER ROLE pulse_platform_readonly SET TimeZone TO 'UTC';

-- search_path для приложения фиксируется здесь, чтобы избежать
-- SQLSTATE 3F000/42883 при hardening-режиме драйвера.
ALTER ROLE pulse_platform_app      SET search_path = pg_catalog, public;
ALTER ROLE pulse_platform_readonly SET search_path = pg_catalog, public;