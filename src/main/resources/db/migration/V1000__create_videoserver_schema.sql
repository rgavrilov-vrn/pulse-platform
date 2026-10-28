CREATE SCHEMA IF NOT EXISTS videoserver;

CREATE TABLE IF NOT EXISTS videoserver.video_servers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS videoserver.cameras (
    id UUID PRIMARY KEY,
    video_server_id UUID NOT NULL REFERENCES videoserver.video_servers(id),
    external_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS videoserver.channels (
    id UUID PRIMARY KEY,
    camera_id UUID NOT NULL REFERENCES videoserver.cameras(id),
    external_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS videoserver.raw_video_events (
    id UUID PRIMARY KEY,
    video_server_id UUID NOT NULL REFERENCES videoserver.video_servers(id),
    camera_id UUID,
    channel_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(64) NOT NULL,
    raw_payload JSONB NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS videoserver.normalized_video_events (
    id UUID PRIMARY KEY,
    raw_event_id UUID NOT NULL UNIQUE REFERENCES videoserver.raw_video_events(id),
    video_server_id UUID NOT NULL,
    camera_id UUID,
    channel_id UUID,
    object_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    payload_version INTEGER NOT NULL,
    normalized_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_raw_video_events_occurred_at
    ON videoserver.raw_video_events(occurred_at);

CREATE INDEX IF NOT EXISTS idx_normalized_video_events_event_type
    ON videoserver.normalized_video_events(event_type);
