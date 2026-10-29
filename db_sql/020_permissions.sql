-- 020_permissions.sql
-- FIX (P0): добавлены гранты на incident, videoserver, ref для приложения
--           и readonly-роли. Ранее приложение не имело доступа к incident.*,
--           videoserver.* и ref.* — первый же SELECT падал.
-- FIX: добавлены гранты на SEQUENCES (incident_number_seq, BIGSERIAL в
--      status_history/audit_log).
-- FIX: для audit выданы только SELECT + INSERT (append-only).
-- FIX: ALTER DEFAULT PRIVILEGES прописан для всех схем и разбит по схемам
--      (ALTER DEFAULT PRIVILEGES не принимает список схем).
--
-- ВАЖНО: скрипт выполняется владельцем БД / суперпользователем —
--        тем же, кто создавал объекты в миграциях. Иначе default privileges
--        не применятся к объектам, созданным другой ролью.

-- ============================================================================
-- pulse_platform_app (read/write на бизнес-схемах)
-- ============================================================================
GRANT USAGE ON SCHEMA auth, ref, incident, videoserver, outbox, audit, goodwan
    TO pulse_platform_app;

GRANT SELECT, INSERT, UPDATE, DELETE
      ON ALL TABLES IN SCHEMA auth, ref, incident, videoserver, outbox, goodwan
          TO pulse_platform_app;

-- audit — append-only: приложению запрещены UPDATE/DELETE.
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA audit TO pulse_platform_app;

-- USAGE, SELECT на все последовательности: BIGSERIAL в incident, audit, outbox
-- и явно созданная incident_number_seq.
GRANT USAGE, SELECT
             ON ALL SEQUENCES IN SCHEMA auth, ref, incident, videoserver, outbox, audit, goodwan
                 TO pulse_platform_app;

-- ============================================================================
-- pulse_platform_readonly (только SELECT на бизнес-данных)
-- ============================================================================
GRANT USAGE ON SCHEMA ref, incident, videoserver, outbox, audit, goodwan
    TO pulse_platform_readonly;

GRANT SELECT
      ON ALL TABLES IN SCHEMA ref, incident, videoserver, outbox, audit, goodwan
          TO pulse_platform_readonly;

-- ============================================================================
-- Default privileges для будущих объектов
-- ============================================================================
ALTER DEFAULT PRIVILEGES IN SCHEMA auth
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA ref
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA incident
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA videoserver
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA outbox
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA goodwan
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT SELECT, INSERT ON TABLES TO pulse_platform_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA auth
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA ref
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA incident
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA videoserver
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA outbox
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA goodwan
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA ref
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA incident
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA videoserver
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA outbox
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA goodwan
    GRANT SELECT ON TABLES TO pulse_platform_readonly;