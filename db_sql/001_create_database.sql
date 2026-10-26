-- 001_create_database.sql
-- Выполняется подключением к системной БД PostgreSQL, например postgres.
-- В PostgreSQL CREATE DATABASE нельзя выполнять внутри транзакции.

CREATE DATABASE pulse_platform
    WITH
    OWNER = pulse_platform_app
    ENCODING = 'UTF8'
    TEMPLATE = template0;

COMMENT ON DATABASE pulse_platform
IS '';

