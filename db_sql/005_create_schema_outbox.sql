
CREATE SCHEMA IF NOT EXISTS outbox;

CREATE TABLE IF NOT EXISTS outbox.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR (20) NOT NULL DEFAULT 'NEW',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_outbox_events_status_next_attempt ON outbox.outbox_events(status, next_attempt_at);

CREATE TABLE IF NOT EXISTS outbox.processed_events
(
    consumer_name VARCHAR(100) NOT NULL,
    event_id      UUID         NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_name, event_id)
    );

CREATE INDEX IF NOT EXISTS ix_processed_events_time
    ON outbox.processed_events (processed_at);

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA outbox IS 'Transactional Outbox: надёжная публикация доменных событий + дедупликация у потребителей.';

COMMENT ON TABLE  outbox.outbox_events IS 'Исходящие события (outbox pattern). Пишутся в одной транзакции с изменением домена, публикуются асинхронно.';
COMMENT ON COLUMN outbox.outbox_events.id              IS 'Идентификатор события (UUID, идемпотентность публикации)';
COMMENT ON COLUMN outbox.outbox_events.event_type      IS 'Тип события (например, IncidentCreated, IncidentStatusChanged)';
COMMENT ON COLUMN outbox.outbox_events.entity_id       IS 'Идентификатор сущности-агрегата, породившей событие';
COMMENT ON COLUMN outbox.outbox_events.payload         IS 'Тело события (JSONB)';
COMMENT ON COLUMN outbox.outbox_events.status          IS 'Статус: NEW / PUBLISHED / FAILED / DEAD';
COMMENT ON COLUMN outbox.outbox_events.attempts        IS 'Количество попыток публикации';
COMMENT ON COLUMN outbox.outbox_events.next_attempt_at IS 'Момент следующей попытки (для retry/backoff)';
COMMENT ON COLUMN outbox.outbox_events.published_at    IS 'Момент успешной публикации (null, пока не опубликовано)';
COMMENT ON COLUMN outbox.outbox_events.last_error      IS 'Текст последней ошибки публикации';
COMMENT ON COLUMN outbox.outbox_events.created_at      IS 'Момент создания события';

COMMENT ON TABLE  outbox.processed_events IS 'Дедупликация у потребителей: какие события уже обработаны каким consumer-ом.';
COMMENT ON COLUMN outbox.processed_events.consumer_name IS 'Имя потребителя (consumer)';
COMMENT ON COLUMN outbox.processed_events.event_id      IS 'Идентификатор обработанного события';
COMMENT ON COLUMN outbox.processed_events.processed_at  IS 'Момент обработки';