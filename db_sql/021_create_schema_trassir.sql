-- TRASSIR telemetry schema.

BEGIN;

CREATE SCHEMA IF NOT EXISTS trassir;

CREATE TABLE IF NOT EXISTS trassir.ingest_raw (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL UNIQUE,
    received_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    producer_name TEXT NOT NULL,
    producer_version TEXT,
    server_id TEXT NOT NULL,
    raw_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS trassir.events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raw_id UUID NOT NULL REFERENCES trassir.ingest_raw(id),
    event_index INTEGER NOT NULL,
    event_id UUID NOT NULL UNIQUE,
    event_type TEXT NOT NULL,
    event_version TEXT NOT NULL,
    occurred_at TIMESTAMPTZ,
    observed_at TIMESTAMPTZ NOT NULL,
    source JSONB NOT NULL,
    data JSONB NOT NULL,
    raw_event JSONB NOT NULL,
    processing_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (raw_id, event_index)
);

CREATE TABLE IF NOT EXISTS trassir.snapshot_streams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id TEXT NOT NULL,
    stream_id TEXT NOT NULL,
    last_snapshot_id UUID,
    last_part_index INTEGER,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (server_id, stream_id)
);

CREATE TABLE IF NOT EXISTS trassir.snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id TEXT NOT NULL,
    stream_id TEXT NOT NULL,
    snapshot_seq BIGINT NOT NULL,
    total_parts INTEGER NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE (server_id, stream_id, snapshot_seq)
);

CREATE TABLE IF NOT EXISTS trassir.snapshot_parts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_id UUID NOT NULL REFERENCES trassir.snapshots(id),
    part_index INTEGER NOT NULL,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    UNIQUE (snapshot_id, part_index)
);

CREATE TABLE IF NOT EXISTS trassir.snapshot_objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_id UUID NOT NULL REFERENCES trassir.snapshots(id),
    object_key TEXT NOT NULL,
    object_payload JSONB NOT NULL,
    UNIQUE (snapshot_id, object_key)
);

CREATE TABLE IF NOT EXISTS trassir.objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id TEXT NOT NULL,
    object_key TEXT NOT NULL,
    object_payload JSONB NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    UNIQUE (server_id, object_key)
);

CREATE TABLE IF NOT EXISTS trassir.system_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id TEXT NOT NULL,
    event_id UUID NOT NULL UNIQUE,
    event_type TEXT NOT NULL,
    event_version TEXT NOT NULL,
    occurred_at TIMESTAMPTZ,
    observed_at TIMESTAMPTZ NOT NULL,
    severity TEXT NOT NULL,
    source JSONB NOT NULL,
    data JSONB NOT NULL,
    raw_event JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS trassir.server_state (
    server_id TEXT PRIMARY KEY,
    last_heartbeat_at TIMESTAMPTZ,
    last_snapshot_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS trassir.agent_heartbeats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id TEXT NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    component TEXT NOT NULL,
    status TEXT NOT NULL,
    details JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_trassir_events_raw_id ON trassir.events (raw_id);
CREATE INDEX IF NOT EXISTS ix_trassir_events_type ON trassir.events (event_type);
CREATE INDEX IF NOT EXISTS ix_trassir_snapshots_server_stream ON trassir.snapshots (server_id, stream_id);
CREATE INDEX IF NOT EXISTS ix_trassir_objects_server_key ON trassir.objects (server_id, object_key);

COMMIT;
