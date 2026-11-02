-- 001_create_database.sql
-- Выполняется подключением к системной БД PostgreSQL (например, postgres).
-- CREATE DATABASE нельзя выполнять внутри транзакции.
-- flyway:executeInTransaction=false

CREATE DATABASE pulse_platform
    WITH
    OWNER    = pulse_platform_app
    ENCODING = 'UTF8'
    TEMPLATE = template0;

COMMENT ON DATABASE pulse_platform
    IS 'Pulse Platform operational database: incidents, video events, auth, refs, outbox, audit.';

-- Следующие миграции (002+) выполняются подключением к pulse_platform.