-- 02_create_database.sql
-- Выполняется суперпользователем, подключение к postgres.
-- CREATE DATABASE нельзя выполнять внутри транзакции.
-- flyway:executeInTransaction=false  (если запускается через Flyway)

CREATE DATABASE pulse_platform
    WITH
    OWNER    = pulse_platform_app
    ENCODING = 'UTF8'
    TEMPLATE = template0;

COMMENT ON DATABASE pulse_platform
    IS 'Pulse Platform operational database.';