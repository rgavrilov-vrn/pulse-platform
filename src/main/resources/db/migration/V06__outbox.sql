-- V06__outbox.sql
SET search_path = pg_catalog, public;

CREATE TABLE outbox.outbox_events (
                                      id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      event_type      VARCHAR(100) NOT NULL,
                                      entity_id       UUID,
                                      payload         JSONB NOT NULL,
                                      status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
                                      attempts        INT NOT NULL DEFAULT 0,
                                      next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      published_at    TIMESTAMPTZ,
                                      last_error      TEXT,
                                      created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      CONSTRAINT chk_outbox_events_status   CHECK (status IN ('NEW','PUBLISHED','FAILED','DEAD')),
                                      CONSTRAINT chk_outbox_events_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_outbox_events_status_next_attempt ON outbox.outbox_events (status, next_attempt_at);
CREATE INDEX ix_outbox_events_created_at          ON outbox.outbox_events (created_at);

CREATE TABLE outbox.processed_events (
                                         consumer_name VARCHAR(100) NOT NULL,
                                         event_id      UUID         NOT NULL,
                                         processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                         PRIMARY KEY (consumer_name, event_id)
);

CREATE INDEX ix_processed_events_time ON outbox.processed_events (processed_at);