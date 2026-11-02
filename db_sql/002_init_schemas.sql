-- 002_init_schemas.sql
-- Выполняется подключением к pulse_platform (а не к системной postgres).
-- CREATE EXTENSION требует прав суперпользователя.
--
-- FIX (SQLSTATE 3F000 на PG 16): миграционные инструменты часто запускают
-- скрипты с search_path = '' (hardening). Неквалифицированный CREATE
-- падает с "no schema has been selected to create in".
-- Поэтому:
--   1) явно ставим search_path;
--   2) явно указываем SCHEMA public у CREATE EXTENSION.
--
-- gen_random_uuid() на PG 13+ живёт в pg_catalog — pgcrypto нужен
-- только ради crypt() / gen_salt().

SET search_path = pg_catalog, public;

CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA public;   -- crypt / gen_salt
CREATE EXTENSION IF NOT EXISTS citext   SCHEMA public;   -- auth.users.login / email