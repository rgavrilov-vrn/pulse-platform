-- V19__trassir_manual_incidents_raw.sql
SET search_path = pg_catalog, public;

CREATE TABLE trassir.manual_incidents_raw (
                                              id              UUID PRIMARY KEY,
                                              event_id        UUID NOT NULL UNIQUE,
                                              occurred_at     TIMESTAMPTZ NOT NULL,
                                              observed_at     TIMESTAMPTZ NOT NULL,
                                              object_id       VARCHAR(255) NOT NULL,
                                              object_name     VARCHAR(255),
                                              object_class    VARCHAR(64),
                                              comment         TEXT,
                                              reason          VARCHAR(100),
                                              video_timestamp VARCHAR(100),
                                              image_file      TEXT,
                                              raw_payload     JSONB NOT NULL,
                                              created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_trassir_manual_incidents_raw_object_id
    ON trassir.manual_incidents_raw (object_id);
CREATE INDEX ix_trassir_manual_incidents_raw_occurred_at
    ON trassir.manual_incidents_raw (occurred_at DESC);

COMMENT ON TABLE trassir.manual_incidents_raw IS
    'Сырые данные ручных инцидентов от TRASSIR-агента (event_type=trassir.incident_manual).';