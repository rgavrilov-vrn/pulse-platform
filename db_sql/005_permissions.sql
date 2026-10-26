-- 005_permissions.sql
-- Выполняется владельцем базы или администратором.

GRANT USAGE ON SCHEMA auth, audit, outbox TO pulse_platform_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth, outbox TO pulse_platform_app;

GRANT SELECT ON ALL TABLES IN SCHEMA audit, outbox TO pulse_platform_app;


GRANT SELECT ON ALL TABLES IN SCHEMA audit, outbox, ref TO pulse_platform_readonly;

ALTER DEFAULT PRIVILEGES IN SCHEMA auth, ref, outbox
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
