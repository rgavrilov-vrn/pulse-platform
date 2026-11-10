-- V11__videoserver.sql
-- Домен видео. Без FK на ref.*: привязка камер к объектам — через incident.context_refs.

SET search_path = pg_catalog, public;

CREATE TABLE videoserver.video_servers (
                                           id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           object_id   UUID,
                                           zone_id     UUID,
                                           source      VARCHAR(64)  NOT NULL,
                                           external_id VARCHAR(255) NOT NULL,
                                           name        VARCHAR(255) NOT NULL,
                                           host        VARCHAR(255),
                                           base_url    VARCHAR(512),
                                           description TEXT,
                                           status      VARCHAR(32),
                                           is_active   BOOLEAN NOT NULL DEFAULT TRUE,
                                           created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                           updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                           CONSTRAINT uq_video_servers_source_external UNIQUE (source, external_id)
);

CREATE TABLE videoserver.cameras (
                                     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     video_server_id UUID REFERENCES videoserver.video_servers(id),
                                     source          VARCHAR(64)  NOT NULL,
                                     external_id     VARCHAR(255),
                                     code            VARCHAR(100),
                                     name            VARCHAR(255) NOT NULL,
                                     description     TEXT,
                                     status          VARCHAR(32),
                                     is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                                     created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_cameras_source_external_notnull
    ON videoserver.cameras (source, external_id) WHERE external_id IS NOT NULL;

CREATE TABLE videoserver.channels (
                                      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      camera_id   UUID NOT NULL REFERENCES videoserver.cameras(id),
                                      external_id VARCHAR(255),
                                      name        VARCHAR(255) NOT NULL,
                                      status      VARCHAR(32),
                                      is_active   BOOLEAN NOT NULL DEFAULT TRUE,
                                      created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_channels_camera_external_nonnull
    ON videoserver.channels (camera_id, external_id) WHERE external_id IS NOT NULL;

CREATE TABLE videoserver.raw_video_events (
                                              id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                              video_server_id   UUID NOT NULL REFERENCES videoserver.video_servers(id),
                                              camera_id         UUID REFERENCES videoserver.cameras(id),
                                              channel_id        UUID REFERENCES videoserver.channels(id),
                                              external_event_id VARCHAR(200) NOT NULL,
                                              occurred_at       TIMESTAMPTZ  NOT NULL,
                                              source            VARCHAR(64)  NOT NULL,
                                              raw_payload       JSONB        NOT NULL,
                                              ingested_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                              CONSTRAINT uk_raw_video_events_server_external UNIQUE (video_server_id, external_event_id)
);

CREATE TABLE videoserver.normalized_video_events (
                                                     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     raw_event_id    UUID NOT NULL UNIQUE REFERENCES videoserver.raw_video_events(id),
                                                     video_server_id UUID NOT NULL REFERENCES videoserver.video_servers(id),
                                                     camera_id       UUID REFERENCES videoserver.cameras(id),
                                                     channel_id      UUID REFERENCES videoserver.channels(id),
                                                     object_id       UUID,
                                                     occurred_at     TIMESTAMPTZ  NOT NULL,
                                                     source          VARCHAR(64)  NOT NULL,
                                                     event_type      VARCHAR(128) NOT NULL,
                                                     severity        VARCHAR(32)  NOT NULL,
                                                     payload_version INTEGER      NOT NULL,
                                                     normalized_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_raw_video_events_occurred_at ON videoserver.raw_video_events (occurred_at);
CREATE INDEX idx_raw_video_events_camera      ON videoserver.raw_video_events (camera_id);
CREATE INDEX idx_raw_video_events_channel     ON videoserver.raw_video_events (channel_id);
CREATE INDEX idx_raw_video_events_ingested_at ON videoserver.raw_video_events (ingested_at);

CREATE INDEX idx_norm_events_event_type       ON videoserver.normalized_video_events (event_type);
CREATE INDEX idx_norm_events_camera_occurred  ON videoserver.normalized_video_events (camera_id, occurred_at);
CREATE INDEX idx_norm_events_server_occurred  ON videoserver.normalized_video_events (video_server_id, occurred_at);
CREATE INDEX idx_norm_events_channel_occurred ON videoserver.normalized_video_events (channel_id, occurred_at);

CREATE TRIGGER trg_video_servers_updated_at BEFORE UPDATE ON videoserver.video_servers FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();
CREATE TRIGGER trg_cameras_updated_at       BEFORE UPDATE ON videoserver.cameras       FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();
CREATE TRIGGER trg_channels_updated_at      BEFORE UPDATE ON videoserver.channels      FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();