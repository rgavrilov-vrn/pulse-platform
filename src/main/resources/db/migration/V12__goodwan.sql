-- V12__goodwan.sql
-- Append-only история запросов/ответов устройства goodwan.

SET search_path = pg_catalog, public;

CREATE TABLE goodwan.raw_data_history (
                                          id                  BIGSERIAL PRIMARY KEY,
                                          external_device_id  VARCHAR(64) NOT NULL,
                                          source_record_id    BIGINT NOT NULL,
                                          endpoint            VARCHAR(128) NOT NULL,
                                          request_parameters  JSONB NOT NULL,
                                          response_item       JSONB NOT NULL,
                                          requested_at        TIMESTAMPTZ NOT NULL,
                                          received_at         TIMESTAMPTZ NOT NULL,
                                          payload_hash        VARCHAR(64) NOT NULL,
                                          created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                                          CONSTRAINT uq_goodwan_raw_data_history UNIQUE (external_device_id, source_record_id),
                                          CONSTRAINT chk_goodwan_payload_hash    CHECK (payload_hash ~ '^[0-9a-fA-F]{64}$')
    );

CREATE INDEX idx_goodwan_raw_data_history_device
    ON goodwan.raw_data_history (external_device_id, source_record_id DESC);
CREATE INDEX idx_goodwan_raw_data_history_created_at
    ON goodwan.raw_data_history (created_at);