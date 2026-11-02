-- =============================================================================
-- 007_create_schema_videoserver.sql
-- -----------------------------------------------------------------------------
-- Домен видео: серверы, камеры, каналы, события.
-- Зависимости: ref.objects, ref.zones (006).
--
-- FIX: uq_cameras_source_external заменён на частичный уникальный индекс
--      WHERE external_id IS NOT NULL. Старый UNIQUE (source, external_id)
--      не защищал от дублирования при NULL external_id (NULL != NULL в PG),
--      что противоречило комментарию о «выполняет роль partial unique index».
-- FIX: удалён избыточный idx_norm_events_raw — он дублировал btree-индекс,
--      автоматически созданный UNIQUE(raw_event_id).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS videoserver;

CREATE TABLE IF NOT EXISTS videoserver.video_servers (
                                                         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    object_id       UUID,
    zone_id         UUID,
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

CREATE TABLE IF NOT EXISTS videoserver.cameras (
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

-- FIX: частичный UNIQUE вместо обычного UNIQUE (source, external_id),
--      чтобы уникальность работала при external_id IS NOT NULL.
--      Камеры без external_id (ручные) могут дублироваться по source — это ок.
CREATE UNIQUE INDEX IF NOT EXISTS uq_cameras_source_external_notnull
    ON videoserver.cameras (source, external_id) WHERE external_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS videoserver.channels (
                                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id   UUID NOT NULL REFERENCES videoserver.cameras(id),
    external_id VARCHAR(255),
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(32),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_channels_camera_external_nonnull
    ON videoserver.channels (camera_id, external_id) WHERE external_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS videoserver.raw_video_events (
                                                            id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_server_id   UUID NOT NULL REFERENCES videoserver.video_servers(id),
    camera_id         UUID REFERENCES videoserver.cameras(id),
    channel_id        UUID REFERENCES videoserver.channels(id),
    external_event_id VARCHAR(200) NOT NULL,
    occurred_at       TIMESTAMPTZ  NOT NULL,
    source            VARCHAR(64)  NOT NULL,
    raw_payload       JSONB        NOT NULL,
    ingested_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_raw_video_events_server_external
    UNIQUE (video_server_id, external_event_id)
    );

CREATE TABLE IF NOT EXISTS videoserver.normalized_video_events (
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

CREATE INDEX IF NOT EXISTS idx_raw_video_events_occurred_at ON videoserver.raw_video_events (occurred_at);
CREATE INDEX IF NOT EXISTS idx_raw_video_events_camera      ON videoserver.raw_video_events (camera_id);
CREATE INDEX IF NOT EXISTS idx_raw_video_events_channel     ON videoserver.raw_video_events (channel_id);

CREATE INDEX IF NOT EXISTS idx_norm_events_event_type       ON videoserver.normalized_video_events (event_type);
CREATE INDEX IF NOT EXISTS idx_norm_events_camera_occurred  ON videoserver.normalized_video_events (camera_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_norm_events_server_occurred  ON videoserver.normalized_video_events (video_server_id, occurred_at);
-- FIX: idx_norm_events_raw удалён — UNIQUE(raw_event_id) уже создаёт btree-индекс.
CREATE INDEX IF NOT EXISTS idx_norm_events_channel_occurred ON videoserver.normalized_video_events (channel_id, occurred_at);

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA videoserver IS 'Домен видео: серверы, камеры, каналы, входящие и нормализованные события.';

COMMENT ON TABLE  videoserver.video_servers IS 'Серверы видео (VMS).';
COMMENT ON COLUMN videoserver.video_servers.object_id       IS 'Объект (nullable, без FK на ref.objects)';
COMMENT ON COLUMN videoserver.video_servers.source      IS 'Код источника (совпадает с RawVideoEventRequest.source)';
COMMENT ON COLUMN videoserver.video_servers.external_id IS 'ID сервера во внешней системе';
COMMENT ON COLUMN videoserver.video_servers.host        IS 'VMS-хост';
COMMENT ON COLUMN videoserver.video_servers.base_url    IS 'HTTP base URL API';
COMMENT ON COLUMN videoserver.video_servers.status      IS 'Runtime-статус: ONLINE / OFFLINE / UNKNOWN';
COMMENT ON COLUMN videoserver.video_servers.is_active   IS 'Сервер активен в справочнике';

COMMENT ON TABLE  videoserver.cameras IS 'Камеры. Одна камера → много каналов (channels). object_id/zone_id — nullable, без FK на ref.';
COMMENT ON COLUMN videoserver.cameras.video_server_id IS 'Сервер, к которому привязана камера';
COMMENT ON COLUMN videoserver.cameras.source          IS 'Код источника (совпадает с RawVideoEventRequest.source)';
COMMENT ON COLUMN videoserver.cameras.external_id     IS 'ID камеры во внешней системе';
COMMENT ON COLUMN videoserver.cameras.code            IS 'Внутренний код камеры';
COMMENT ON COLUMN videoserver.cameras.status          IS 'Runtime-статус: ONLINE / OFFLINE / UNKNOWN';

COMMENT ON TABLE  videoserver.channels IS 'Каналы камеры. У одной камеры — много каналов.';
COMMENT ON COLUMN videoserver.channels.camera_id   IS 'Родительская камера';
COMMENT ON COLUMN videoserver.channels.external_id IS 'ID канала во внешней системе';
COMMENT ON COLUMN videoserver.channels.status      IS 'Runtime-статус канала';

COMMENT ON TABLE  videoserver.raw_video_events IS 'Сырые события от VMS. Append-only, идемпотентность через (video_server_id, external_event_id).';
COMMENT ON COLUMN videoserver.raw_video_events.external_event_id IS 'ID события во внешней системе (идемпотентность)';
COMMENT ON COLUMN videoserver.raw_video_events.raw_payload       IS 'Исходный JSON от VMS (append-only, для replay/аудита)';
COMMENT ON COLUMN videoserver.raw_video_events.ingested_at       IS 'Момент приёма в систему';

COMMENT ON TABLE  videoserver.normalized_video_events IS 'Нормализованные события (проекция над raw). Источник для чтения/API.';
COMMENT ON COLUMN videoserver.normalized_video_events.raw_event_id    IS 'Ссылка на сырое событие (1:1)';
COMMENT ON COLUMN videoserver.normalized_video_events.object_id       IS 'Объект (nullable, без FK на ref.objects)';
COMMENT ON COLUMN videoserver.normalized_video_events.event_type      IS 'Классифицированный тип события';
COMMENT ON COLUMN videoserver.normalized_video_events.severity        IS 'Классифицированная серьёзность';
COMMENT ON COLUMN videoserver.normalized_video_events.payload_version IS 'Версия правил классификатора';
COMMENT ON COLUMN videoserver.normalized_video_events.normalized_at   IS 'Момент нормализации';