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
    UNIQUE (external_device_id, source_record_id)
    );

CREATE INDEX IF NOT EXISTS idx_goodwan_raw_data_history_device
    ON goodwan.raw_data_history (external_device_id, source_record_id DESC);
