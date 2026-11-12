-- V13__trassir.sql
SET search_path = pg_catalog, public;

-- ---------- Ingest ----------
CREATE TABLE trassir.ingest_raw (
                                    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    batch_id         UUID NOT NULL UNIQUE,
                                    received_at      TIMESTAMPTZ NOT NULL,
                                    sent_at          TIMESTAMPTZ,
                                    producer_name    TEXT,
                                    producer_version TEXT,
                                    server_id        TEXT NOT NULL,
                                    raw_body         JSONB NOT NULL,
                                    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_trassir_ingest_raw_created_at ON trassir.ingest_raw (created_at);
CREATE INDEX ix_trassir_ingest_raw_server_id  ON trassir.ingest_raw (server_id);

-- ---------- Events ----------
CREATE TABLE trassir.events (
                                id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                raw_id           UUID NOT NULL REFERENCES trassir.ingest_raw(id),
                                server_id        TEXT NOT NULL,
                                event_index      INTEGER NOT NULL,
                                event_id         UUID NOT NULL UNIQUE,
                                event_type       TEXT NOT NULL,
                                event_version    TEXT NOT NULL,
                                occurred_at      TIMESTAMPTZ,
                                observed_at      TIMESTAMPTZ NOT NULL,
                                source           JSONB NOT NULL,
                                data             JSONB NOT NULL,
                                raw_event        JSONB NOT NULL,
                                processing_error TEXT,
                                status           VARCHAR NOT NULL DEFAULT 'PENDING',
                                last_error       TEXT,
                                retry_count      INTEGER NOT NULL DEFAULT 0,
                                next_retry_at    TIMESTAMPTZ,
                                created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                UNIQUE (raw_id, event_index)
);

CREATE INDEX ix_trassir_events_raw_id            ON trassir.events (raw_id);
CREATE INDEX ix_trassir_events_type              ON trassir.events (event_type);
CREATE INDEX ix_trassir_events_server_id         ON trassir.events (server_id);
CREATE INDEX ix_trassir_events_status_next_retry ON trassir.events (status, next_retry_at);
CREATE INDEX ix_trassir_events_created_at        ON trassir.events (created_at);

-- ---------- Snapshots ----------
CREATE TABLE trassir.snapshot_streams (
                                          id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          server_id        TEXT NOT NULL,
                                          stream_id        TEXT NOT NULL,
                                          last_snapshot_id UUID,
                                          last_part_index  INTEGER,
                                          updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                                          UNIQUE (server_id, stream_id)
);

-- FIX #30 (бывший V16): stream_id = стабильный идентификатор потока (обычно server_id),
-- НЕ snapshot UUID. Иначе таблица растёт 1:1 со снапшотами.
COMMENT ON COLUMN trassir.snapshot_streams.stream_id IS
    'Стабильный идентификатор потока (обычно server_id). Не snapshot UUID.';

CREATE TABLE trassir.snapshots (
                                   id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   external_snapshot_id UUID NOT NULL UNIQUE,
                                   server_id            TEXT NOT NULL,
                                   stream_id            TEXT NOT NULL,
                                   snapshot_seq         BIGINT NOT NULL,
                                   total_parts          INTEGER NOT NULL,
                                   received_at          TIMESTAMPTZ NOT NULL,
                                   completed_at         TIMESTAMPTZ,
                                   last_error           TEXT,
                                   next_retry_at        TIMESTAMPTZ,
                                   retry_count          INTEGER NOT NULL DEFAULT 0,
                                   status               VARCHAR(32) NOT NULL DEFAULT 'PENDING'
);

CREATE UNIQUE INDEX ux_trassir_snapshots_external_snapshot_id
    ON trassir.snapshots (external_snapshot_id);
CREATE INDEX ix_trassir_snapshots_server_stream
    ON trassir.snapshots (server_id, stream_id);
CREATE INDEX ix_trassir_snapshots_status_next_retry
    ON trassir.snapshots (status, next_retry_at);

CREATE TABLE trassir.snapshot_parts (
                                        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        snapshot_id UUID NOT NULL REFERENCES trassir.snapshots(id),
                                        part_index  INTEGER NOT NULL,
                                        payload     JSONB NOT NULL,
                                        received_at TIMESTAMPTZ NOT NULL,
                                        UNIQUE (snapshot_id, part_index)
);

CREATE TABLE trassir.snapshot_objects (
                                          id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          snapshot_id    UUID NOT NULL REFERENCES trassir.snapshots(id),
                                          object_key     TEXT NOT NULL,
                                          object_payload JSONB NOT NULL,
                                          UNIQUE (snapshot_id, object_key)
);

CREATE TABLE trassir.objects (
                                 id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 server_id      TEXT NOT NULL,
                                 object_key     TEXT NOT NULL,
                                 object_payload JSONB NOT NULL,
                                 first_seen_at  TIMESTAMPTZ NOT NULL,
                                 last_seen_at   TIMESTAMPTZ NOT NULL,
                                 deleted_at     TIMESTAMPTZ,
                                 UNIQUE (server_id, object_key)
);

CREATE INDEX ix_trassir_objects_server_key ON trassir.objects (server_id, object_key);

-- ---------- System events / state ----------
CREATE TABLE trassir.system_events (
                                       id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       server_id     TEXT NOT NULL,
                                       event_id      UUID NOT NULL UNIQUE,
                                       event_type    TEXT NOT NULL,
                                       event_version TEXT NOT NULL,
                                       occurred_at   TIMESTAMPTZ,
                                       observed_at   TIMESTAMPTZ NOT NULL,
                                       severity      TEXT NOT NULL,
                                       source        JSONB NOT NULL,
                                       data          JSONB NOT NULL,
                                       raw_event     JSONB NOT NULL,
                                       created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_trassir_system_events_created_at ON trassir.system_events (created_at);

CREATE TABLE trassir.server_state (
                                      server_id         TEXT PRIMARY KEY,
                                      last_heartbeat_at TIMESTAMPTZ,
                                      last_snapshot_at  TIMESTAMPTZ,
                                      updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE trassir.agent_heartbeats (
                                          id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          server_id   TEXT NOT NULL,
                                          observed_at TIMESTAMPTZ NOT NULL,
                                          component   TEXT NOT NULL,
                                          status      TEXT,
                                          details     JSONB NOT NULL,
                                          created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_trassir_agent_heartbeats_server ON trassir.agent_heartbeats (server_id, observed_at DESC);

-- ---------- Admin audit ----------
CREATE TABLE trassir.admin_audit_log (
                                         id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         actor      TEXT NOT NULL,
                                         action     TEXT NOT NULL,
                                         target     TEXT NOT NULL,
                                         details    JSONB,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_admin_audit_log_created_at ON trassir.admin_audit_log (created_at DESC);