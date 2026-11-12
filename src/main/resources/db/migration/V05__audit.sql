-- V05__audit.sql
SET search_path = pg_catalog, public;

CREATE TABLE audit.audit_log (
                                 id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                 occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                 actor_id      UUID         REFERENCES auth.users(id),
                                 action        VARCHAR(100) NOT NULL,
                                 entity_schema VARCHAR(100) NOT NULL,
                                 entity_table  VARCHAR(100) NOT NULL,
                                 entity_id     UUID,
                                 old_data      JSONB,
                                 new_data      JSONB,
                                 request_id    VARCHAR(100),
                                 source_ip     INET,
                                 user_agent    TEXT
);

CREATE INDEX ix_audit_log_occurred_at ON audit.audit_log (occurred_at DESC);
CREATE INDEX ix_audit_log_entity      ON audit.audit_log (entity_schema, entity_table, entity_id);
CREATE INDEX ix_audit_log_actor       ON audit.audit_log (actor_id, occurred_at DESC);
CREATE INDEX ix_audit_log_action      ON audit.audit_log (action, occurred_at DESC);
-- Примечание: отдельный индекс на created_at не нужен —
-- ix_audit_log_occurred_at (occurred_at DESC) покрывает и retention-очистку,
-- и выборки по occurred_at (B-tree двунаправленный).