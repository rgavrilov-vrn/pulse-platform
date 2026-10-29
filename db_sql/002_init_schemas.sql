-- 002_init_schemas.sql
-- FIX: явно зафиксировано, что миграция выполняется подключением к pulse_platform,
--      а не к системной postgres. CREATE EXTENSION требует прав суперпользователя,
--      поэтому скрипт запускается суперпользователем.
--
-- Для psql удобно:
--     \connect pulse_platform
-- Для Flyway/Liquibase: скрипт должен быть привязан к pulse_platform через
--      конфигурацию миграционного инструмента (default schema = public этой БД).

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;    -- auth.users.login / email