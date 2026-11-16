-- V18__trassir_ingest_log.sql
-- Журнал всех HTTP-запросов на /api/v1/trassir/ingest.
-- Пишется ВСЕГДА: успех, битый JSON, дубликат, oversized, внутренняя ошибка.
-- Не является audit-log (это admin_audit_log), это request/response log для диагностики.
SET search_path = pg_catalog, public;

CREATE TABLE trassir.ingest_log (
                                    id                      UUID PRIMARY KEY,
                                    received_at             TIMESTAMPTZ NOT NULL,
                                    duration_ms             INTEGER NOT NULL,

    -- Request (HTTP)
                                    http_method             VARCHAR(10) NOT NULL,
                                    request_uri             TEXT NOT NULL,
                                    remote_addr             INET,
                                    x_forwarded_for         TEXT,
                                    content_type            VARCHAR(128),
                                    content_length          BIGINT,
                                    user_agent              TEXT,
                                    auth_token_present      BOOLEAN NOT NULL DEFAULT FALSE,
                                    correlation_id          VARCHAR(100),

    -- Request body (усечённо)
                                    request_body_prefix     TEXT,
                                    request_body_bytes      BIGINT,
                                    request_body_sha256     CHAR(64),
                                    request_body_truncated  BOOLEAN NOT NULL DEFAULT FALSE,
                                    request_body_read       BOOLEAN NOT NULL DEFAULT TRUE,

    -- Response
                                    http_status             INTEGER NOT NULL,
                                    response_code           VARCHAR(64),
                                    response_message        TEXT,
                                    response_body           TEXT,

    -- Relations (nullable: если парсинг упал — не заполняются)
                                    batch_id                UUID,
                                    raw_id                  UUID,
                                    server_id               UUID,
                                    event_count             INTEGER,

                                    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_trassir_ingest_log_received_at_desc
    ON trassir.ingest_log (received_at DESC);
CREATE INDEX ix_trassir_ingest_log_batch_id
    ON trassir.ingest_log (batch_id) WHERE batch_id IS NOT NULL;
CREATE INDEX ix_trassir_ingest_log_server_id
    ON trassir.ingest_log (server_id) WHERE server_id IS NOT NULL;
CREATE INDEX ix_trassir_ingest_log_http_status
    ON trassir.ingest_log (http_status);
CREATE INDEX ix_trassir_ingest_log_response_code
    ON trassir.ingest_log (response_code) WHERE response_code IS NOT NULL;
CREATE INDEX ix_trassir_ingest_log_correlation_id
    ON trassir.ingest_log (correlation_id) WHERE correlation_id IS NOT NULL;

COMMENT ON TABLE  trassir.ingest_log IS
    'Журнал HTTP-запросов на /api/v1/trassir/ingest. Пишется всегда, независимо от результата.';
COMMENT ON COLUMN trassir.ingest_log.request_body_prefix IS
    'Первые 8KB тела (UTF-8). NULL если тело не прочитано.';
COMMENT ON COLUMN trassir.ingest_log.request_body_sha256 IS
    'SHA-256 всего тела (даже если request_body_prefix усечён) — для сверки с ingest_raw.raw_body.';
COMMENT ON COLUMN trassir.ingest_log.request_body_truncated IS
    'TRUE если request_body_prefix усечён (реальный размер — request_body_bytes).';
COMMENT ON COLUMN trassir.ingest_log.response_body IS
    'JSON тела ответа для 4xx/5xx (ErrorResponse). NULL для 200.';
COMMENT ON COLUMN trassir.ingest_log.auth_token_present IS
    'Флаг наличия Authorization: Bearer (сам токен НЕ логируется).';