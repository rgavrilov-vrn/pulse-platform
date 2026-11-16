-- V17__trassir_admin_operations.sql
SET search_path = pg_catalog, public;

-- ============================================================
-- 1. trassir.system_events — материализованные поля для фильтрации/фронта
-- ============================================================
ALTER TABLE trassir.system_events
    ADD COLUMN object_id   VARCHAR(255),
    ADD COLUMN object_name VARCHAR(255),
    ADD COLUMN p1          TEXT,
    ADD COLUMN p2          TEXT,
    ADD COLUMN p3          TEXT;

UPDATE trassir.system_events
SET object_id   = source::jsonb->>'object_id',
    object_name = source::jsonb->>'object_name',
    p1          = data::jsonb->>'p1',
    p2          = data::jsonb->>'p2',
    p3          = data::jsonb->>'p3';

CREATE INDEX ix_trassir_system_events_object_id
    ON trassir.system_events (object_id) WHERE object_id IS NOT NULL;
CREATE INDEX ix_trassir_system_events_object_name
    ON trassir.system_events (object_name) WHERE object_name IS NOT NULL;
CREATE INDEX ix_trassir_system_events_event_type
    ON trassir.system_events (event_type);
CREATE INDEX ix_trassir_system_events_observed_at_desc
    ON trassir.system_events (observed_at DESC);

COMMENT ON COLUMN trassir.system_events.object_id   IS 'source->>object_id (TRASSIR SDK object_key камеры/сервера)';
COMMENT ON COLUMN trassir.system_events.object_name IS 'source->>object_name';
COMMENT ON COLUMN trassir.system_events.p1          IS 'data->>p1 (параметр события)';
COMMENT ON COLUMN trassir.system_events.p2          IS 'data->>p2';
COMMENT ON COLUMN trassir.system_events.p3          IS 'data->>p3';

-- ============================================================
-- 2. trassir.admin_audit_log — расширение
-- ============================================================
ALTER TABLE trassir.admin_audit_log
    ADD COLUMN correlation_id VARCHAR(100),
    ADD COLUMN request_id     VARCHAR(100),
    ADD COLUMN status         VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN error_message  TEXT,
    ADD COLUMN affected_count INTEGER;

CREATE INDEX ix_trassir_admin_audit_log_action_created
    ON trassir.admin_audit_log (action, created_at DESC);
CREATE INDEX ix_trassir_admin_audit_log_actor_created
    ON trassir.admin_audit_log (actor, created_at DESC);

COMMENT ON COLUMN trassir.admin_audit_log.status IS 'SUCCESS | FAILED | PARTIAL';
COMMENT ON COLUMN trassir.admin_audit_log.affected_count IS 'Сколько строк изменено/удалено операцией';