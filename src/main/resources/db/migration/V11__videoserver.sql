-- V11__videoserver.sql
-- raw_video_events удалена: raw data живёт в trassir.ingest_raw / trassir.events.
-- Идемпотентность нормализованных событий: UNIQUE (video_server_id, external_event_id).
SET search_path = pg_catalog, public;

-- ============================================================
-- video_servers
-- ============================================================
CREATE TABLE videoserver.video_servers (
                                           id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           object_id       UUID,                                -- ref.objects.id (facility)
                                           zone_id         UUID,                                -- ref.zones.id
                                           source          VARCHAR(64)  NOT NULL,               -- 'trassir' / 'other'
                                           external_id     VARCHAR(255),                        -- для trassir = NULL, PK = id (UUID)
                                           name            VARCHAR(255) NOT NULL,
                                           host            VARCHAR(255),
                                           base_url        VARCHAR(512),
                                           description     TEXT,
                                           status          VARCHAR(32),
                                           last_seen_at    TIMESTAMPTZ,
                                           deleted_at      TIMESTAMPTZ,
                                           deactivated_by  VARCHAR(32),
                                           is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                                           created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                           updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                           CONSTRAINT uq_video_servers_source_external UNIQUE (source, external_id)
);

COMMENT ON COLUMN videoserver.video_servers.external_id IS
    'Внешний ID от вендора. Для source=''trassir'' = NULL: идентификатор — id (UUID).';
COMMENT ON COLUMN videoserver.video_servers.object_id IS
    'ref.objects.id — площадка (facility). Используется для фильтра «события по площадке».';
COMMENT ON COLUMN videoserver.video_servers.last_seen_at IS
    'Последний heartbeat/snapshot от сервера.';
COMMENT ON COLUMN videoserver.video_servers.deactivated_by IS
    '''manual'' — выключено вручную; ''agent_snapshot'' — отсутствовало в snapshot.';

-- ============================================================
-- cameras
-- ============================================================
CREATE TABLE videoserver.cameras (
                                     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     video_server_id UUID NOT NULL REFERENCES videoserver.video_servers(id),
                                     source          VARCHAR(64)  NOT NULL,
                                     external_id     VARCHAR(255),                        -- = TRASSIR object_key
                                     code            VARCHAR(100),
                                     name            VARCHAR(255) NOT NULL,
                                     description     TEXT,
                                     status          VARCHAR(32),
                                     is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                                     last_seen_at    TIMESTAMPTZ,
                                     deleted_at      TIMESTAMPTZ,
                                     deactivated_by  VARCHAR(32),
                                     created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Уникальность в рамках сервера: object_key='cam1' на двух серверах = разные камеры
CREATE UNIQUE INDEX uq_cameras_server_external_notnull
    ON videoserver.cameras (video_server_id, external_id) WHERE external_id IS NOT NULL;

CREATE INDEX ix_cameras_server_active
    ON videoserver.cameras (video_server_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_cameras_last_seen
    ON videoserver.cameras (last_seen_at DESC) WHERE deleted_at IS NULL;

COMMENT ON COLUMN videoserver.cameras.external_id IS
    'TRASSIR object_key. Уникален в рамках video_server_id.';
COMMENT ON COLUMN videoserver.cameras.deactivated_by IS
    '''manual'' — выключено вручную; ''agent_snapshot'' — отсутствовало в snapshot.';

-- ============================================================
-- channels
-- ============================================================
CREATE TABLE videoserver.channels (
                                      id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      camera_id    UUID NOT NULL REFERENCES videoserver.cameras(id),
                                      external_id  VARCHAR(255),
                                      name         VARCHAR(255) NOT NULL,
                                      status       VARCHAR(32),
                                      is_active    BOOLEAN NOT NULL DEFAULT TRUE,
                                      last_seen_at TIMESTAMPTZ,
                                      created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_channels_camera_external_nonnull
    ON videoserver.channels (camera_id, external_id) WHERE external_id IS NOT NULL;

-- ============================================================
-- normalized_video_events (единственное хранилище событий в videoserver)
-- ============================================================
CREATE TABLE videoserver.normalized_video_events (
                                                     id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     video_server_id   UUID NOT NULL REFERENCES videoserver.video_servers(id),
                                                     camera_id         UUID REFERENCES videoserver.cameras(id),
                                                     channel_id        UUID REFERENCES videoserver.channels(id),
                                                     object_id         UUID,                              -- reserved: facility link (future)
                                                     source_event_id   UUID NOT NULL,                     -- = trassir.events.event_id
                                                     external_event_id VARCHAR(255) NOT NULL,             -- idempotency key (from agent)
                                                     occurred_at       TIMESTAMPTZ NOT NULL,
                                                     source            VARCHAR(64) NOT NULL,
                                                     event_type        VARCHAR(128) NOT NULL,
                                                     severity          VARCHAR(32) NOT NULL,
                                                     payload_version   INTEGER NOT NULL,
                                                     normalized_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                     ingested_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_norm_events_server_external
    ON videoserver.normalized_video_events (video_server_id, external_event_id);
CREATE INDEX ix_norm_events_source_event
    ON videoserver.normalized_video_events (source_event_id);
CREATE INDEX idx_norm_events_event_type
    ON videoserver.normalized_video_events (event_type);
CREATE INDEX idx_norm_events_camera_occurred
    ON videoserver.normalized_video_events (camera_id, occurred_at);
CREATE INDEX idx_norm_events_server_occurred
    ON videoserver.normalized_video_events (video_server_id, occurred_at);
CREATE INDEX idx_norm_events_channel_occurred
    ON videoserver.normalized_video_events (channel_id, occurred_at);

COMMENT ON COLUMN videoserver.normalized_video_events.source_event_id IS
    'UUID исходного события из trassir.events.event_id (не FK: trassir — независимый модуль).';
COMMENT ON COLUMN videoserver.normalized_video_events.external_event_id IS
    'Внешний ID события от агента. Уникален в рамках video_server_id.';
COMMENT ON TABLE videoserver.normalized_video_events IS
    'Нормализованные видео-события. Raw-данные — в trassir.ingest_raw / trassir.events.';

-- ============================================================
-- Триггеры updated_at
-- ============================================================
CREATE TRIGGER trg_video_servers_updated_at BEFORE UPDATE ON videoserver.video_servers
    FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();
CREATE TRIGGER trg_cameras_updated_at BEFORE UPDATE ON videoserver.cameras
    FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();
CREATE TRIGGER trg_channels_updated_at BEFORE UPDATE ON videoserver.channels
    FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();