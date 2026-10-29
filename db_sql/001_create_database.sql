-- 001_create_database.sql
-- Выполняется подключением к системной БД PostgreSQL (например, postgres).
-- CREATE DATABASE нельзя выполнять внутри транзакции.

CREATE DATABASE pulse_platform
    WITH
    OWNER = pulse_platform_app
    ENCODING = 'UTF8'
    TEMPLATE = template0;

-- FIX: пустая строка в COMMENT заменена осмысленным описанием.
COMMENT ON DATABASE pulse_platform
    IS 'Pulse Platform operational database: incidents, video events, auth, refs, outbox, audit.';

-- FIX: явно предупреждаем — следующие миграции (002+) должны выполняться
-- уже подключением к pulse_platform, а не к postgres.