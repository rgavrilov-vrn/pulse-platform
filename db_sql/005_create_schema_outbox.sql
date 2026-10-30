-- 005_create_schema_outbox.sql
-- FIX: добавлены CHECK-констрейнты на status и attempts (по аналогии
--      с incident.source_events и incident.report_jobs).

CREATE SCHEMA IF NOT EXISTS outbox;

CREATE TABLE IF NOT EXISTS outbox.outbox_events (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_outbox_events_status   CHECK (status IN ('NEW', 'PUBLISHED', 'FAILED', 'DEAD')),
    CONSTRAINT chk_outbox_events_attempts CHECK (attempts >= 0)
    );

CREATE INDEX IF NOT EXISTS ix_outbox_events_status_next_attempt
    ON outbox.outbox_events (status, next_attempt_at);

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
COMMENT ON COLUMN outbox.outbox_events.attempts        IS 'Количество попыток публикации (>= 0)';
COMMENT ON COLUMN outbox.outbox_events.next_attempt_at IS 'Момент следующей попытки (для retry/backoff)';
COMMENT ON COLUMN outbox.outbox_events.published_at    IS 'Момент успешной публикации (null, пока не опубликовано)';
COMMENT ON COLUMN outbox.outbox_events.last_error      IS 'Текст последней ошибки публикации';
COMMENT ON COLUMN outbox.outbox_events.created_at      IS 'Момент создания события';

COMMENT ON TABLE  outbox.processed_events IS 'Дедупликация у потребителей: какие события уже обработаны каким consumer-ом.';
COMMENT ON COLUMN outbox.processed_events.consumer_name IS 'Имя потребителя (consumer)';
COMMENT ON COLUMN outbox.processed_events.event_id      IS 'Идентификатор обработанного события';
COMMENT ON COLUMN outbox.processed_events.processed_at  IS 'Момент обработки';


-- 009_fix_audit_outbox.sql
-- -------------------------------------------------------------
-- Фикс для auth-событий без привязки к сущности:
--   USER_AUTHENTICATION_FAILED, USER_LOGGED_OUT — entity_id = null.
-- Плюс синхронизация CHECK статуса outbox с Java-enum {NEW, PUBLISHED, FAILED, DEAD}.
--
-- Файл должен идти после 005_create_schema_outbox.sql и 004_create_schema_audit.sql.
-- -------------------------------------------------------------

-- 1. audit.audit_log.entity_id → nullable
ALTER TABLE audit.audit_log
    ALTER COLUMN entity_id DROP NOT NULL;

-- 2. outbox.outbox_events.entity_id → nullable
ALTER TABLE outbox.outbox_events
    ALTER COLUMN entity_id DROP NOT NULL;

-- 3. CHECK статусов outbox — уже совпадает с 005 (NEW/PUBLISHED/FAILED/DEAD),
--    но переопределяем на случай, если в чьём-то окружении 005 была с SENT.
ALTER TABLE outbox.outbox_events
DROP CONSTRAINT IF EXISTS chk_outbox_events_status;

ALTER TABLE outbox.outbox_events
    ADD CONSTRAINT chk_outbox_events_status
        CHECK (status IN ('NEW', 'PUBLISHED', 'FAILED', 'DEAD'));

-- 4. Индекс под частые запросы аудита (был отмечен как P1).
CREATE INDEX IF NOT EXISTS ix_audit_log_actor
    ON audit.audit_log (actor_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_log_action
    ON audit.audit_log (action, occurred_at DESC);