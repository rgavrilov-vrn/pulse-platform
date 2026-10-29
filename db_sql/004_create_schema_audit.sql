-- 003_create_schema_audit.sql

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.audit_log (
    id BIGSERIAL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id UUID REFERENCES auth.users(id),
    action VARCHAR(100) NOT NULL,
    entity_schema VARCHAR(100) NOT NULL,
    entity_table VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    old_data JSONB,
    new_data JSONB,
    request_id VARCHAR(100),
    source_ip INET,
    user_agent TEXT,
    PRIMARY KEY (id, occurred_at)
    ) PARTITION BY RANGE (occurred_at);

CREATE TABLE IF NOT EXISTS audit.audit_log_2026 PARTITION OF audit.audit_log FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS audit.audit_log_default PARTITION OF audit.audit_log DEFAULT;

CREATE INDEX IF NOT EXISTS ix_audit_log_entity ON audit.audit_log (entity_schema, entity_table, entity_id);
CREATE INDEX IF NOT EXISTS ix_audit_log_occurred_at ON audit.audit_log (occurred_at DESC);
