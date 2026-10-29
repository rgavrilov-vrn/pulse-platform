-- =============================================================================
-- 008_create_schema_goodwan.sql
-- -----------------------------------------------------------------------------
-- FIX: добавлен заголовок, COMMENT ON для схемы/таблицы/колонок,
--      CHECK на hex-64 для payload_hash (унификация с media_assets.sha256).
-- Сырые данные внешнего устройства goodwan (append-only history).
-- Зависимостей нет.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS goodwan;

CREATE TABLE IF NOT EXISTS goodwan.raw_data_history (
                                                        id                    BIGSERIAL PRIMARY KEY,
                                                        external_device_id    VARCHAR(64) NOT NULL,
    source_record_id      BIGINT NOT NULL,
    endpoint              VARCHAR(128) NOT NULL,
    request_parameters    JSONB NOT NULL,
    response_item         JSONB NOT NULL,
    requested_at          TIMESTAMPTZ NOT NULL,
    received_at           TIMESTAMPTZ NOT NULL,
    payload_hash          VARCHAR(64) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_goodwan_raw_data_history
    UNIQUE (external_device_id, source_record_id),
    -- FIX: добавлена проверка формата payload_hash (hex-64, как у media_assets.sha256).
    CONSTRAINT chk_goodwan_payload_hash
    CHECK (payload_hash ~ '^[0-9a-fA-F]{64}$')
    );

CREATE INDEX IF NOT EXISTS idx_goodwan_raw_data_history_device
    ON goodwan.raw_data_history (external_device_id, source_record_id DESC);

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA goodwan IS 'Сырые данные внешнего устройства goodwan (append-only история запросов/ответов).';

COMMENT ON TABLE  goodwan.raw_data_history IS 'Append-only история запросов/ответов к устройству; идемпотентность через (external_device_id, source_record_id).';
COMMENT ON COLUMN goodwan.raw_data_history.external_device_id IS 'ID внешнего устройства';
COMMENT ON COLUMN goodwan.raw_data_history.source_record_id   IS 'ID записи на стороне устройства (идемпотентность)';
COMMENT ON COLUMN goodwan.raw_data_history.endpoint           IS 'Логический endpoint устройства (например, /api/records)';
COMMENT ON COLUMN goodwan.raw_data_history.request_parameters IS 'Параметры запроса (JSONB)';
COMMENT ON COLUMN goodwan.raw_data_history.response_item      IS 'Элемент ответа устройства (JSONB)';
COMMENT ON COLUMN goodwan.raw_data_history.requested_at       IS 'Момент запроса';
COMMENT ON COLUMN goodwan.raw_data_history.received_at        IS 'Момент получения ответа';
COMMENT ON COLUMN goodwan.raw_data_history.payload_hash       IS 'SHA-256 (hex) от payload для контроля целостности';
COMMENT ON COLUMN goodwan.raw_data_history.created_at         IS 'Момент записи в БД';