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


-- 1. новая колонка
ALTER TABLE videoserver.raw_video_events
    ADD COLUMN external_event_id VARCHAR(200);

-- 2. backfill для существующих записей (если есть) — синтетический ключ
UPDATE videoserver.raw_video_events
SET external_event_id = id::text
WHERE external_event_id IS NULL;

-- 3. NOT NULL
ALTER TABLE videoserver.raw_video_events
    ALTER COLUMN external_event_id SET NOT NULL;

-- 4. уникальность
ALTER TABLE videoserver.raw_video_events
    ADD CONSTRAINT uk_raw_video_events_server_external
        UNIQUE (video_server_id, external_event_id);

-- 5. индексы (полезно)
CREATE INDEX idx_raw_video_events_camera    ON videoserver.raw_video_events (camera_id);
CREATE INDEX idx_raw_video_events_occurred  ON videoserver.raw_video_events (occurred_at);
CREATE INDEX idx_norm_events_camera_occurred   ON videoserver.normalized_video_events (camera_id, occurred_at);
CREATE INDEX idx_norm_events_server_occurred   ON videoserver.normalized_video_events (video_server_id, occurred_at);
CREATE INDEX idx_norm_events_raw               ON videoserver.normalized_video_events (raw_event_id);