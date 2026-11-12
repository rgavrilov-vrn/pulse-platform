-- V14__grants.sql
-- Единственное место со всеми GRANT'ами.

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

-- trassir — явные гранты (append-only для ingest_raw/events/system_events/admin_audit_log)
GRANT SELECT, INSERT ON trassir.ingest_raw  TO pulse_platform_app;
GRANT SELECT, INSERT ON trassir.events      TO pulse_platform_app;
GRANT UPDATE (processing_error, status, last_error, retry_count, next_retry_at)
    ON trassir.events TO pulse_platform_app;

GRANT SELECT, INSERT ON trassir.system_events TO pulse_platform_app;
GRANT SELECT, INSERT, UPDATE ON
    trassir.snapshot_streams,
    trassir.snapshots,
    trassir.snapshot_parts,
    trassir.snapshot_objects,
    trassir.objects,
    trassir.agent_heartbeats,
    trassir.server_state
    TO pulse_platform_app;

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
-- Explicit EXECUTE на функции (на случай будущего REVOKE ALL FROM PUBLIC)
-- ============================================================================
GRANT EXECUTE ON FUNCTION public.tg_set_updated_at()             TO pulse_platform_app;
GRANT EXECUTE ON FUNCTION incident.generate_incident_number()    TO pulse_platform_app;
GRANT EXECUTE ON FUNCTION incident.set_incident_number()         TO pulse_platform_app;
GRANT EXECUTE ON FUNCTION auth.user_has_permission(UUID, VARCHAR) TO pulse_platform_app;
GRANT EXECUTE ON FUNCTION auth.user_has_permission(UUID, VARCHAR) TO pulse_platform_readonly;

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

-- trassir: безопасный дефолт — SELECT, INSERT.
-- Таблицы, требующие UPDATE/DELETE, должны выдавать грант явно (как V14 выше для snapshot-таблиц).
ALTER DEFAULT PRIVILEGES IN SCHEMA trassir
    GRANT SELECT, INSERT ON TABLES TO pulse_platform_app;
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
-- pg_trgm GIN для audit search (FIX #71 — исправлено)
-- ============================================================================
-- ВАЖНО: AuditQueryService делает OR из LIKE по отдельным колонкам:
--   lower(action) LIKE :p OR lower(request_id) LIKE :p OR ...
-- Индекс на конкатенации (lower(a)||' '||...) при таком запросе НЕ используется.
-- Решение: 5 per-column GIN-индексов. PostgreSQL умеет bitmap OR по ним.
-- Альтернатива: переписать query на lower(concat_ws(' ', a, b, ...)) LIKE :p
-- и оставить один GIN на конкатенации. Требует изменения кода.
CREATE INDEX IF NOT EXISTS ix_audit_log_action_trgm
    ON audit.audit_log USING GIN (lower(action) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_audit_log_request_id_trgm
    ON audit.audit_log USING GIN (lower(coalesce(request_id,'')) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_audit_log_user_agent_trgm
    ON audit.audit_log USING GIN (lower(coalesce(user_agent,'')) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_audit_log_entity_schema_trgm
    ON audit.audit_log USING GIN (lower(entity_schema) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_audit_log_entity_table_trgm
    ON audit.audit_log USING GIN (lower(entity_table) gin_trgm_ops);