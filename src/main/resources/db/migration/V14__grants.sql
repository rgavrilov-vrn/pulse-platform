-- V14__grants.sql
-- Единственное место со всеми GRANT'ами. Приложение под pulse_platform_app (владелец БД),
-- readonly — отдельная роль только с SELECT.
--
-- Явно всё перечислено (не только ALTER DEFAULT PRIVILEGES), чтобы:
--   * проходило на уже созданных объектах;
--   * гранты не зависели от того, под кем выполнялись V01–V13.

-- ============================================================================
-- pulse_platform_app
-- ============================================================================
GRANT USAGE ON SCHEMA
auth, audit, outbox, ref, incident, videoserver, goodwan, trassir
    TO pulse_platform_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA
    auth, outbox, ref, incident, videoserver, goodwan
    TO pulse_platform_app;

-- audit — append-only
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA audit TO pulse_platform_app;

-- trassir — явные гранты (находки #7, #8)
GRANT SELECT, INSERT ON trassir.ingest_raw      TO pulse_platform_app;
GRANT SELECT, INSERT ON trassir.events          TO pulse_platform_app;
GRANT UPDATE (processing_error, status, last_error, retry_count, next_retry_at)
    ON trassir.events TO pulse_platform_app;

GRANT SELECT, INSERT ON trassir.system_events   TO pulse_platform_app;
GRANT SELECT, INSERT, UPDATE ON
    trassir.snapshot_streams,
    trassir.snapshots,
    trassir.snapshot_parts,
    trassir.snapshot_objects,
    trassir.objects,
    trassir.agent_heartbeats,
    trassir.server_state
    TO pulse_platform_app;

-- admin_audit_log (находка #7)
GRANT SELECT, INSERT ON trassir.admin_audit_log TO pulse_platform_app;

-- Последовательности
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA
    auth, audit, outbox, ref, incident, videoserver, goodwan, trassir
    TO pulse_platform_app;

-- ============================================================================
-- pulse_platform_readonly
-- ============================================================================
GRANT USAGE ON SCHEMA ref, incident, videoserver, outbox, audit, goodwan, trassir
    TO pulse_platform_readonly;

GRANT SELECT ON ALL TABLES IN SCHEMA
    ref, incident, videoserver, outbox, audit, goodwan, trassir
    TO pulse_platform_readonly;

-- ============================================================================
-- Default privileges для будущих объектов
-- ============================================================================
DO $$
DECLARE s TEXT;
BEGIN
    FOREACH s IN ARRAY ARRAY['auth','outbox','ref','incident','videoserver','goodwan']
    LOOP
        EXECUTE format(
            'ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app', s);
EXECUTE format(
        'ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app', s);
END LOOP;
END $$;

ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT SELECT, INSERT ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;

-- trassir — дефолты (находка #10)
ALTER DEFAULT PRIVILEGES IN SCHEMA trassir
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA trassir
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;

-- readonly
DO $$
DECLARE s TEXT;
BEGIN
    FOREACH s IN ARRAY ARRAY['ref','incident','videoserver','outbox','audit','goodwan','trassir']
    LOOP
        EXECUTE format(
            'ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT ON TABLES TO pulse_platform_readonly', s);
END LOOP;
END $$;

-- ============================================================================
-- pg_trgm GIN для audit search (находка #71)
-- ============================================================================
CREATE INDEX IF NOT EXISTS ix_audit_log_q_trgm ON audit.audit_log
    USING GIN ((
    lower(action) || ' ' ||
    lower(coalesce(request_id,'')) || ' ' ||
    lower(coalesce(user_agent,'')) || ' ' ||
    lower(entity_schema) || ' ' ||
    lower(entity_table)
    ) gin_trgm_ops);

-- ============================================================================
-- Триггеры updated_at для auth.users уже созданы в V02,
-- для ref.*/videoserver.*/incident.* — в V07/V09/V11.
-- Здесь — только проверка, что все таблицы с updated_at покрыты триггерами.
-- (пропускаем — уже в V07/V09/V11/V02)
-- ============================================================================