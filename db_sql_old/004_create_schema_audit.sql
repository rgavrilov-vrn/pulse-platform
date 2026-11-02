-- 004_create_schema_audit.sql
-- Аудит действий: append-only журнал. Зависит от auth.users (003).
--
-- Партиционирование удалено. Composite PK (id, occurred_at) больше не нужен,
-- id — единственный PK. Для ранее партиционированных БД нужен миграционный
-- скрипт (см. заметку в конце файла) или reset в dev.

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_log (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor_id      UUID         REFERENCES auth.users(id),
    action        VARCHAR(100) NOT NULL,
    entity_schema VARCHAR(100) NOT NULL,
    entity_table  VARCHAR(100) NOT NULL,
    entity_id     UUID,
    old_data      JSONB,
    new_data      JSONB,
    request_id    VARCHAR(100),
    source_ip     INET,
    user_agent    TEXT
);

CREATE INDEX ix_audit_log_occurred_at
    ON audit.audit_log (occurred_at DESC);
CREATE INDEX ix_audit_log_entity
    ON audit.audit_log (entity_schema, entity_table, entity_id);
CREATE INDEX ix_audit_log_actor
    ON audit.audit_log (actor_id, occurred_at DESC);
CREATE INDEX ix_audit_log_action
    ON audit.audit_log (action, occurred_at DESC);

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA audit IS
    'Аудит действий: append-only журнал изменений сущностей.';
COMMENT ON TABLE  audit.audit_log IS
    'Журнал аудита (append-only).';
COMMENT ON COLUMN audit.audit_log.id            IS 'Идентификатор записи (UUID, генерируется на клиенте)';
COMMENT ON COLUMN audit.audit_log.occurred_at   IS 'Момент события';
COMMENT ON COLUMN audit.audit_log.actor_id      IS 'Пользователь-инициатор (nullable — системные действия)';
COMMENT ON COLUMN audit.audit_log.action        IS 'Действие (AuditAction.value)';
COMMENT ON COLUMN audit.audit_log.entity_schema IS 'Схема изменённой сущности';
COMMENT ON COLUMN audit.audit_log.entity_table  IS 'Таблица изменённой сущности';
COMMENT ON COLUMN audit.audit_log.entity_id     IS 'ID сущности (nullable — auth-события без сущности)';
COMMENT ON COLUMN audit.audit_log.old_data      IS 'Состояние до изменения (JSONB), null для CREATE';
COMMENT ON COLUMN audit.audit_log.new_data      IS 'Состояние после изменения (JSONB), null для DELETE';
COMMENT ON COLUMN audit.audit_log.request_id    IS 'Идентификатор запроса (корреляция с логами)';
COMMENT ON COLUMN audit.audit_log.source_ip     IS 'IP-адрес источника';
COMMENT ON COLUMN audit.audit_log.user_agent    IS 'User-Agent клиента';