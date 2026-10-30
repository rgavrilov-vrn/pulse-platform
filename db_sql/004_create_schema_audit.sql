-- 004_create_schema_audit.sql
-- FIX: заголовок приведён к фактическому номеру файла (было -- 003_...).
-- FIX: добавлена заметка о необходимости автоматического создания партиций.
-- Зависит от auth.users (003).

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.audit_log (
                                               id BIGSERIAL,
                                               occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id UUID REFERENCES auth.users(id),
    action VARCHAR(100) NOT NULL,
    entity_schema VARCHAR(100) NOT NULL,
    entity_table VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    old_data JSONB,
    new_data JSONB,
    request_id VARCHAR(100),
    source_ip INET,
    user_agent TEXT,
    PRIMARY KEY (id, occurred_at)
    ) PARTITION BY RANGE (occurred_at);

CREATE TABLE IF NOT EXISTS audit.audit_log_2026 PARTITION OF audit.audit_log FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS audit.audit_log_default PARTITION OF audit.audit_log DEFAULT;

CREATE INDEX IF NOT EXISTS ix_audit_log_entity ON audit.audit_log (entity_schema, entity_table, entity_id);
CREATE INDEX IF NOT EXISTS ix_audit_log_occurred_at ON audit.audit_log (occurred_at DESC);

-- FIX: заметка о будущих партициях.
-- В продакшене рекомендуется pg_partman либо ежегодная миграция,
-- создающая партицию на следующий год. Иначе все записи вне диапазона
-- попадут в audit_log_default, что ухудшает partition pruning.

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA audit IS 'Аудит действий: append-only журнал изменений сущностей. Партиционирован по occurred_at.';

COMMENT ON TABLE  audit.audit_log IS 'Журнал аудита (append-only). Партиционирован по occurred_at (RANGE).';
COMMENT ON COLUMN audit.audit_log.id             IS 'Порядковый номер записи (BIGSERIAL, в составе PK с occurred_at)';
COMMENT ON COLUMN audit.audit_log.occurred_at    IS 'Момент события (ключ партиционирования)';
COMMENT ON COLUMN audit.audit_log.actor_id       IS 'Пользователь-инициатор (nullable — системные действия)';
COMMENT ON COLUMN audit.audit_log.action         IS 'Действие: CREATE / UPDATE / DELETE / LOGIN / ...';
COMMENT ON COLUMN audit.audit_log.entity_schema  IS 'Схема изменённой сущности';
COMMENT ON COLUMN audit.audit_log.entity_table   IS 'Таблица изменённой сущности';
COMMENT ON COLUMN audit.audit_log.entity_id      IS 'Идентификатор изменённой сущности (UUID)';
COMMENT ON COLUMN audit.audit_log.old_data       IS 'Состояние до изменения (JSONB), null для CREATE';
COMMENT ON COLUMN audit.audit_log.new_data       IS 'Состояние после изменения (JSONB), null для DELETE';
COMMENT ON COLUMN audit.audit_log.request_id     IS 'Идентификатор запроса (корреляция с логами)';
COMMENT ON COLUMN audit.audit_log.source_ip      IS 'IP-адрес источника';
COMMENT ON COLUMN audit.audit_log.user_agent     IS 'User-Agent клиента';

COMMENT ON TABLE  audit.audit_log_2026        IS 'Партиция audit_log за 2026 год.';
COMMENT ON TABLE  audit.audit_log_default     IS 'Default-партиция audit_log (fallback для дат вне объявленных диапазонов).';

-- 009_audit_log_pk_to_uuid.sql
-- -------------------------------------------------------------
-- audit.audit_log.id: BIGSERIAL → UUID.
--
-- Зачем: DDL объявляет PRIMARY KEY (id, occurred_at) — обязательное
-- условие для партиционированной таблицы. JPA не может смоделировать
-- @GeneratedValue(IDENTITY) с composite PK. Переход на UUID генерирует
-- id на клиенте (Hibernate @UuidGenerator), что совместимо с composite.
--
-- ВНИМАНИЕ: id — surrogate, нигде не используется как FK. DROP COLUMN
-- безопасен. На больших таблицах ALTER ADD COLUMN с DEFAULT может
-- выполняться долго — на проде применять через maintenance window.
-- -------------------------------------------------------------

BEGIN;

ALTER TABLE audit.audit_log DROP CONSTRAINT IF EXISTS audit_log_pkey;

ALTER TABLE audit.audit_log DROP COLUMN IF EXISTS id;

ALTER TABLE audit.audit_log
    ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE audit.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id, occurred_at);

DROP SEQUENCE IF EXISTS audit.audit_log_id_seq;

COMMIT;