CREATE SCHEMA IF NOT EXISTS incident;

CREATE TABLE IF NOT EXISTS incident.incidents (
    id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL UNIQUE,
    source_event_type VARCHAR(100) NOT NULL,
    video_server_id UUID,
    camera_id UUID,
    channel_id UUID,
    object_id UUID,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    resolution TEXT
);

CREATE INDEX IF NOT EXISTS idx_incidents_status ON incident.incidents(status);
CREATE INDEX IF NOT EXISTS idx_incidents_occurred_at ON incident.incidents(occurred_at);
CREATE INDEX IF NOT EXISTS idx_incidents_camera ON incident.incidents(camera_id);
