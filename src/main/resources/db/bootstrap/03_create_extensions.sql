-- 03_create_extensions.sql
-- Выполняется суперпользователем, подключение к pulse_platform.
-- CREATE EXTENSION требует суперюзера; вынесено из Flyway.

SET search_path = pg_catalog, public;

CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA public;   -- crypt / gen_salt / gen_random_uuid
CREATE EXTENSION IF NOT EXISTS citext   SCHEMA public;   -- регистронезависимые login/email
CREATE EXTENSION IF NOT EXISTS pg_trgm  SCHEMA public;   -- GIN для audit search (см. V14)