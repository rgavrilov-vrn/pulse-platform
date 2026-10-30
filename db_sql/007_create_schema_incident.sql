-- =============================================================================
-- 007_create_schema_incident.sql
-- FIX: заголовок приведён к фактическому номеру файла (было -- 002_...).
-- FIX: version DEFAULT 1 → DEFAULT 0 — согласовано с Hibernate @Version seed=0
--      и с CHECK (version >= 0).
-- FIX: добавлены комментарии к source_events.external_event_id / idempotency_key.
-- -----------------------------------------------------------------------------
-- Инцидент — корневая доменно-нейтральная сущность.
-- НЕ содержит полей о месте, камере, сервере, сотруднике, отделе и т.п.
-- Доменные привязки — через incident.context_refs.
--
-- Зависимости: auth.users (003) + incident.sources seed.
-- Порядок: 003 (auth) → 007 (incident).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS incident;

-- -----------------------------------------------------------------------------
-- Нумерация инцидентов
-- -----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS incident.incident_number_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE FUNCTION incident.generate_incident_number()
RETURNS VARCHAR LANGUAGE plpgsql AS $$
DECLARE next_number BIGINT;
BEGIN
    next_number := nextval('incident.incident_number_seq');
RETURN 'INC-' || to_char(current_date, 'YYYY') || '-' || lpad(next_number::TEXT, 6, '0');
END;
$$;

-- -----------------------------------------------------------------------------
-- sources
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident.sources (
                                                code       VARCHAR(64)  PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

INSERT INTO incident.sources(code, name) VALUES
                                             ('videoserver', 'Video Server'),
                                             ('manual',      'Manual')
    ON CONFLICT (code) DO NOTHING;

-- -----------------------------------------------------------------------------
-- incidents
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident.incidents (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number VARCHAR(30) NOT NULL UNIQUE,
    -- FIX: DEFAULT 0 вместо 1 — согласовано с Hibernate @Version seed=0.
    version INTEGER NOT NULL DEFAULT 0,
    source VARCHAR(64) NOT NULL REFERENCES incident.sources(code),
    creation_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    event_time TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES auth.users(id),
    updated_by UUID REFERENCES auth.users(id),
    operator_id UUID REFERENCES auth.users(id),
    assigned_to UUID REFERENCES auth.users(id),
    due_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    confirmed_by UUID REFERENCES auth.users(id),
    closed_at TIMESTAMPTZ,
    closed_by UUID REFERENCES auth.users(id),
    resolution_comment TEXT,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID REFERENCES auth.users(id),
    cancellation_reason TEXT,
    comment TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_incidents_version       CHECK (version >= 0),
    CONSTRAINT chk_incidents_dates         CHECK (updated_at >= created_at),
    CONSTRAINT chk_incidents_due_date      CHECK (due_at IS NULL OR due_at >= created_at),
    CONSTRAINT chk_incidents_creation_mode CHECK (creation_mode IN ('INTEGRATION','MANUAL','IMPORT')),
    CONSTRAINT chk_incidents_status        CHECK (status IN ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED')),
    CONSTRAINT chk_incidents_severity      CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))
    );

-- -----------------------------------------------------------------------------
-- Функции для триггеров
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION incident.set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION incident.set_incident_number()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.number IS NULL OR NEW.number = '' THEN
        NEW.number := incident.generate_incident_number();
END IF;
RETURN NEW;
END;
$$;

-- -----------------------------------------------------------------------------
-- Триггеры
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'trg_incidents_updated_at'
          AND tgrelid = 'incident.incidents'::regclass
          AND NOT tgisinternal
    ) THEN
DROP TRIGGER trg_incidents_updated_at ON incident.incidents;
END IF;
END $$;

CREATE TRIGGER trg_incidents_updated_at
    BEFORE UPDATE ON incident.incidents
    FOR EACH ROW EXECUTE FUNCTION incident.set_updated_at();

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'trg_set_incident_number'
          AND tgrelid = 'incident.incidents'::regclass
          AND NOT tgisinternal
    ) THEN
DROP TRIGGER trg_set_incident_number ON incident.incidents;
END IF;
END $$;

CREATE TRIGGER trg_set_incident_number
    BEFORE INSERT ON incident.incidents
    FOR EACH ROW EXECUTE FUNCTION incident.set_incident_number();

-- -----------------------------------------------------------------------------
-- context_refs
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident.context_refs (
                                                     incident_id          UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE CASCADE,
    domain               VARCHAR(64)  NOT NULL,
    entity_type          VARCHAR(64)  NOT NULL,
    entity_id            UUID         NOT NULL,
    is_primary           BOOLEAN      NOT NULL DEFAULT FALSE,
    entity_name_snapshot VARCHAR(255),
    metadata             JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (incident_id, domain, entity_type, entity_id),
    CONSTRAINT chk_context_refs_domain      CHECK (domain      ~ '^[a-z][a-z0-9_]*$'),
    CONSTRAINT chk_context_refs_entity_type CHECK (entity_type ~ '^[a-z][a-z0-9_]*$')
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_context_refs_primary
    ON incident.context_refs (incident_id, domain) WHERE is_primary;
CREATE INDEX IF NOT EXISTS ix_context_refs_entity
    ON incident.context_refs (domain, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS ix_context_refs_metadata_gin
    ON incident.context_refs USING GIN (metadata);

-- -----------------------------------------------------------------------------
-- Справочники инцидента
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident.violation_categories (
                                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS incident.checklist_items (
                                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES incident.violation_categories(id),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    requires_image BOOLEAN NOT NULL DEFAULT FALSE,
    requires_comment BOOLEAN NOT NULL DEFAULT FALSE,
    requires_video BOOLEAN NOT NULL DEFAULT FALSE,
    default_severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    comment_template TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_checklist_items_severity
    CHECK (default_severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))
    );

CREATE TABLE IF NOT EXISTS incident.status_transitions (
                                                           from_status VARCHAR(32) NOT NULL,
    to_status   VARCHAR(32) NOT NULL,
    PRIMARY KEY (from_status, to_status),
    CONSTRAINT chk_status_transitions_from
    CHECK (from_status IN ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED')),
    CONSTRAINT chk_status_transitions_to
    CHECK (to_status   IN ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED'))
    );

-- -----------------------------------------------------------------------------
-- incident_checklist_items
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident.incident_checklist_items (
                                                                 incident_id            UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    checklist_item_id      UUID NOT NULL REFERENCES incident.checklist_items(id) ON DELETE RESTRICT,
    item_name_snapshot     VARCHAR(500) NOT NULL,
    category_name_snapshot VARCHAR(255),
    severity               VARCHAR(32) NOT NULL,
    comment                TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             UUID NOT NULL REFERENCES auth.users(id),
    PRIMARY KEY (incident_id, checklist_item_id),
    CONSTRAINT chk_incident_checklist_items_severity
    CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))
    );

-- -----------------------------------------------------------------------------
-- source_events
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident.source_events (
                                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(64) NOT NULL REFERENCES incident.sources(code),
    external_event_id VARCHAR(500),
    idempotency_key VARCHAR(500),
    incident_id UUID REFERENCES incident.incidents(id),
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_source_events_status      CHECK (processing_status IN ('RECEIVED','PROCESSED','FAILED','DUPLICATE')),
    CONSTRAINT chk_source_events_retry_count CHECK (retry_count >= 0)
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_source_events_external_id
    ON incident.source_events (source, external_event_id) WHERE external_event_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_source_events_idempotency_key
    ON incident.source_events (idempotency_key) WHERE idempotency_key IS NOT NULL;

-- -----------------------------------------------------------------------------
-- media_assets / incident_materials / archive_references / comments /
-- assignments / status_history
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident.media_assets (
                                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_provider VARCHAR(50) NOT NULL DEFAULT 'S3',
    storage_bucket VARCHAR(255),
    storage_key VARCHAR(1000) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    file_extension VARCHAR(20),
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    material_type VARCHAR(32) NOT NULL,
    captured_at TIMESTAMPTZ,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploaded_by UUID NOT NULL REFERENCES auth.users(id),
    is_original BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_media_storage_key UNIQUE (storage_provider, storage_key),
    CONSTRAINT chk_media_size   CHECK (size_bytes >= 0),
    CONSTRAINT chk_media_sha256 CHECK (sha256 ~ '^[0-9a-fA-F]{64}$'),
    CONSTRAINT chk_media_material_type
    CHECK (material_type IN ('IMAGE','VIDEO','DOCUMENT','OTHER'))
    );

CREATE TABLE IF NOT EXISTS incident.incident_materials (
                                                           incident_id    UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    media_asset_id UUID NOT NULL REFERENCES incident.media_assets(id) ON DELETE RESTRICT,
    role           VARCHAR(32) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES auth.users(id),
    PRIMARY KEY (incident_id, media_asset_id),
    CONSTRAINT chk_incident_materials_role
    CHECK (role IN ('PRIMARY_EVIDENCE','ADDITIONAL_EVIDENCE','ARCHIVE_FRAGMENT','REPORT_ATTACHMENT'))
    );

CREATE TABLE IF NOT EXISTS incident.archive_references (
                                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    reference_type VARCHAR(30) NOT NULL,
    reference_value TEXT NOT NULL,
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    source VARCHAR(64) NOT NULL REFERENCES incident.sources(code),
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    last_checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES auth.users(id),
    CONSTRAINT chk_archive_reference_type   CHECK (reference_type IN ('URL','NATIVE_LINK','EVENT_ID','FILE')),
    CONSTRAINT chk_archive_reference_period CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)
    );

CREATE TABLE IF NOT EXISTS incident.comments (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    author_id UUID NOT NULL REFERENCES auth.users(id),
    comment_type VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES auth.users(id),
    CONSTRAINT chk_comments_body CHECK (length(trim(body)) > 0),
    CONSTRAINT chk_comments_type
    CHECK (comment_type IN ('OPERATOR','REVIEW','RESOLUTION','SYSTEM'))
    );

CREATE TABLE IF NOT EXISTS incident.assignments (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    assigned_to UUID NOT NULL REFERENCES auth.users(id),
    assigned_by UUID NOT NULL REFERENCES auth.users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    comment TEXT,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_assignment_due_date CHECK (due_at IS NULL OR due_at >= assigned_at)
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_current_incident_assignment
    ON incident.assignments (incident_id) WHERE is_current = TRUE;

CREATE TABLE IF NOT EXISTS incident.status_history (
                                                       id BIGSERIAL PRIMARY KEY,
                                                       incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    from_status VARCHAR(32),
    to_status   VARCHAR(32) NOT NULL,
    changed_by UUID NOT NULL REFERENCES auth.users(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    comment TEXT,
    request_id VARCHAR(100),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_status_history_from
    CHECK (from_status IS NULL OR from_status IN ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED')),
    CONSTRAINT chk_status_history_to
    CHECK (to_status IN ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED'))
    );

-- -----------------------------------------------------------------------------
-- Индексы
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS ix_incidents_event_time        ON incident.incidents (event_time DESC);
CREATE INDEX IF NOT EXISTS ix_incidents_created_at        ON incident.incidents (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_incidents_status            ON incident.incidents (status);
CREATE INDEX IF NOT EXISTS ix_incidents_source            ON incident.incidents (source);
CREATE INDEX IF NOT EXISTS ix_incidents_operator          ON incident.incidents (operator_id);
CREATE INDEX IF NOT EXISTS ix_incidents_assigned_to       ON incident.incidents (assigned_to);
CREATE INDEX IF NOT EXISTS ix_incidents_severity_status   ON incident.incidents (severity, status);
CREATE INDEX IF NOT EXISTS ix_incidents_status_event_time ON incident.incidents (status, event_time DESC);
CREATE INDEX IF NOT EXISTS ix_incidents_metadata_gin      ON incident.incidents USING GIN (metadata);

CREATE INDEX IF NOT EXISTS ix_checklist_items_category
    ON incident.checklist_items (category_id, sort_order) WHERE is_active;

CREATE INDEX IF NOT EXISTS ix_source_events_payload_gin   ON incident.source_events USING GIN (payload);
CREATE INDEX IF NOT EXISTS ix_incident_materials_incident ON incident.incident_materials (incident_id);
CREATE INDEX IF NOT EXISTS ix_status_history_incident     ON incident.status_history (incident_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS ix_comments_incident           ON incident.comments (incident_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- View
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW incident.incident_list AS
SELECT
    i.id, i.number, i.source, i.creation_mode, i.status, i.severity,
    i.event_time, i.created_at, i.updated_at,
    operator_user.display_name AS operator_name,
    assigned_user.display_name AS assigned_to_name,
    (SELECT COUNT(*) FROM incident.incident_materials       im  WHERE im.incident_id  = i.id) AS material_count,
    (SELECT COUNT(*) FROM incident.incident_checklist_items ici WHERE ici.incident_id = i.id) AS checklist_count,
    (SELECT jsonb_object_agg(cr.domain, jsonb_build_object(
            'entityType', cr.entity_type,
            'entityId',   cr.entity_id,
            'entityName', cr.entity_name_snapshot))
     FROM incident.context_refs cr
     WHERE cr.incident_id = i.id AND cr.is_primary) AS primary_context
FROM incident.incidents i
         LEFT JOIN auth.users operator_user ON operator_user.id = i.operator_id
         LEFT JOIN auth.users assigned_user ON assigned_user.id = i.assigned_to;

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA incident IS 'Инциденты: корневая доменно-нейтральная сущность + собственные справочники.';

COMMENT ON TABLE  incident.sources IS 'Реестр источников инцидентов. Каждый домен регистрирует свой код при старте.';
COMMENT ON COLUMN incident.sources.code       IS 'Код источника: videoserver, hr, warehouse, manual, ...';
COMMENT ON COLUMN incident.sources.name       IS 'Человекочитаемое имя источника';
COMMENT ON COLUMN incident.sources.is_active  IS 'Источник активен (можно создавать новые инциденты)';
COMMENT ON COLUMN incident.sources.created_at IS 'Момент регистрации источника';

COMMENT ON TABLE  incident.incidents IS 'Инцидент. Не содержит привязок к месту/камере/сотруднику — они в context_refs.';
COMMENT ON COLUMN incident.incidents.number              IS 'Человекочитаемый номер (INC-YYYY-NNNNNN), генерируется триггером';
COMMENT ON COLUMN incident.incidents.version             IS 'Версия для оптимистичной блокировки (@Version seed=0)';
COMMENT ON COLUMN incident.incidents.source              IS 'Код источника (FK на incident.sources)';
COMMENT ON COLUMN incident.incidents.creation_mode       IS 'Способ создания: INTEGRATION / MANUAL / IMPORT';
COMMENT ON COLUMN incident.incidents.status              IS 'Текущий статус';
COMMENT ON COLUMN incident.incidents.severity            IS 'Серьёзность';
COMMENT ON COLUMN incident.incidents.event_time          IS 'Момент события (когда произошло)';
COMMENT ON COLUMN incident.incidents.received_at         IS 'Момент приёма события из источника';
COMMENT ON COLUMN incident.incidents.created_by          IS 'Пользователь, создавший инцидент (system user для интеграций)';
COMMENT ON COLUMN incident.incidents.updated_by          IS 'Пользователь, последним изменивший инцидент';
COMMENT ON COLUMN incident.incidents.operator_id         IS 'Оператор инцидента';
COMMENT ON COLUMN incident.incidents.assigned_to         IS 'Текущий ответственный';
COMMENT ON COLUMN incident.incidents.due_at              IS 'Дедлайн рассмотрения';
COMMENT ON COLUMN incident.incidents.confirmed_at        IS 'Момент подтверждения';
COMMENT ON COLUMN incident.incidents.confirmed_by        IS 'Кто подтвердил';
COMMENT ON COLUMN incident.incidents.closed_at           IS 'Момент закрытия';
COMMENT ON COLUMN incident.incidents.closed_by           IS 'Кто закрыл';
COMMENT ON COLUMN incident.incidents.resolution_comment  IS 'Комментарий при закрытии';
COMMENT ON COLUMN incident.incidents.cancelled_at        IS 'Момент отмены';
COMMENT ON COLUMN incident.incidents.cancelled_by        IS 'Кто отменил';
COMMENT ON COLUMN incident.incidents.cancellation_reason IS 'Причина отмены';
COMMENT ON COLUMN incident.incidents.comment             IS 'Свободное описание инцидента';
COMMENT ON COLUMN incident.incidents.metadata            IS 'Расширяемые атрибуты (JSONB, GIN-индекс)';

COMMENT ON TABLE  incident.context_refs IS 'Доменно-нейтральные ссылки инцидента на сущности (камера, объект, сотрудник, ...).';
COMMENT ON COLUMN incident.context_refs.domain               IS 'Домен-владелец: videoserver, hr, warehouse, iot, ref, ...';
COMMENT ON COLUMN incident.context_refs.entity_type          IS 'Тип сущности в домене: camera, channel, object, employee, ...';
COMMENT ON COLUMN incident.context_refs.entity_id            IS 'Идентификатор сущности (FK не enforced — межсхемная связь)';
COMMENT ON COLUMN incident.context_refs.is_primary           IS 'Основная сущность домена для инцидента (не более одной)';
COMMENT ON COLUMN incident.context_refs.entity_name_snapshot IS 'Имя сущности на момент создания (историческая правда)';
COMMENT ON COLUMN incident.context_refs.metadata             IS 'Дополнительные атрибуты ссылки';

COMMENT ON TABLE  incident.violation_categories IS 'Категории нарушений (перенесено из ref.violation_categories).';
COMMENT ON COLUMN incident.violation_categories.code       IS 'Уникальный код категории';
COMMENT ON COLUMN incident.violation_categories.sort_order IS 'Порядок сортировки в UI';

COMMENT ON TABLE  incident.checklist_items IS 'Пункты чек-листа (перенесено из ref.checklist_items).';
COMMENT ON COLUMN incident.checklist_items.category_id      IS 'Категория нарушения';
COMMENT ON COLUMN incident.checklist_items.default_severity IS 'Серьёзность по умолчанию';
COMMENT ON COLUMN incident.checklist_items.comment_template IS 'Шаблон комментария по умолчанию';

COMMENT ON TABLE  incident.status_transitions IS 'Матрица разрешённых переходов статусов инцидента.';
COMMENT ON COLUMN incident.status_transitions.from_status IS 'Из какого статуса';
COMMENT ON COLUMN incident.status_transitions.to_status   IS 'В какой статус';

COMMENT ON TABLE  incident.incident_checklist_items IS 'Снапшот чек-листа инцидента. FK на incident.checklist_items + snapshot-имена.';
COMMENT ON COLUMN incident.incident_checklist_items.checklist_item_id      IS 'Ссылка на пункт чек-листа (FK на incident.checklist_items)';
COMMENT ON COLUMN incident.incident_checklist_items.item_name_snapshot     IS 'Имя пункта на момент создания';
COMMENT ON COLUMN incident.incident_checklist_items.category_name_snapshot IS 'Имя категории на момент создания';

COMMENT ON TABLE  incident.source_events IS 'Входящие события источников (до/после обработки).';
COMMENT ON COLUMN incident.source_events.source             IS 'Код источника (FK на incident.sources)';
-- FIX: явно разведены семантики двух ключей дедупликации.
COMMENT ON COLUMN incident.source_events.external_event_id  IS 'Натуральный ID события во внешней системе (для дедупликации, если есть)';
COMMENT ON COLUMN incident.source_events.idempotency_key    IS 'Клиентский ключ идемпотентности (альтернатива external_event_id при at-least-once доставке)';
COMMENT ON COLUMN incident.source_events.incident_id        IS 'Созданный инцидент (null, пока не обработано)';
COMMENT ON COLUMN incident.source_events.processing_status  IS 'RECEIVED / PROCESSED / FAILED / DUPLICATE';
COMMENT ON COLUMN incident.source_events.retry_count        IS 'Количество попыток обработки';

COMMENT ON TABLE  incident.media_assets       IS 'Медиа-активы (файлы в S3).';
COMMENT ON COLUMN incident.media_assets.sha256 IS 'SHA-256 файла (hex, 64 символа)';

COMMENT ON TABLE  incident.incident_materials   IS 'Связка инцидент ↔ медиа-актив с ролью.';
COMMENT ON TABLE  incident.archive_references   IS 'Ссылки на архивы (URL, native link, event id, file).';
COMMENT ON TABLE  incident.comments             IS 'Комментарии к инциденту (оператор, ревью, резолюция, система).';
COMMENT ON TABLE  incident.assignments          IS 'История назначений ответственного (одно current).';
COMMENT ON TABLE  incident.status_history       IS 'История переходов статусов.';

-- =============================================================================
-- 008_add_category_groups.sql
-- Новый справочник "группы категорий" (Событие / Нарушение) + привязка
-- violation_categories → category_groups.
-- Идемпотентно: IF NOT EXISTS / WHERE group_id IS NULL.
-- =============================================================================

CREATE TABLE IF NOT EXISTS incident.category_groups (
                                                        id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(100) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    icon       VARCHAR(100),
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

-- seed двух групп
INSERT INTO incident.category_groups (code, name, icon, sort_order) VALUES
                                                                        ('EVENT',     'Событие',   'event',     10),
                                                                        ('VIOLATION', 'Нарушение', 'violation', 20)
    ON CONFLICT (code) DO NOTHING;

-- backfill: всем существующим категориям — VIOLATION по умолчанию
ALTER TABLE incident.violation_categories
    ADD COLUMN IF NOT EXISTS group_id UUID REFERENCES incident.category_groups(id);

UPDATE incident.violation_categories
SET group_id = (SELECT id FROM incident.category_groups WHERE code = 'VIOLATION')
WHERE group_id IS NULL;

ALTER TABLE incident.violation_categories
    ALTER COLUMN group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_violation_categories_group
    ON incident.violation_categories (group_id, sort_order);

COMMENT ON TABLE  incident.category_groups IS 'Верхнеуровневые группы категорий: Событие / Нарушение.';
COMMENT ON COLUMN incident.category_groups.icon IS 'Имя иконки для UI (optional).';


-- =============================================================================
-- 009_seed_incident_catalog.sql
-- Seed справочника инцидентов:
--   Группа 1: Уведомления (EVENT)
--     └ Категория: Общая (EVENT_GENERAL)
--         ├ Чек-лист: Технические              (TECHNICAL)
--         ├ Чек-лист: Инфраструктурные         (INFRASTRUCTURE)
--         └ Чек-лист: Нарушения регламента     (REGULATION_VIOLATIONS)
--   Группа 2: Нарушения (VIOLATION)
--     └ Категория: Биобезопасность (VIOLATION_BIOSAFETY)
--         ├ Чек-лист: Санитарные обработки               (SANITARY_TREATMENTS)
--         ├ Чек-лист: Транспорт: расхождение по весу     (TRANSPORT_WEIGHT_DISCREPANCY)
--         └ Чек-лист: Транспортные операции              (TRANSPORT_OPERATIONS)
--
-- Идемпотентно: ON CONFLICT (code) DO UPDATE — правки текста/сортировки
-- подхватываются при перезапуске, id сущностей не меняются.
-- Зависит от: 008_add_category_groups.sql
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Группы
-- -----------------------------------------------------------------------------
INSERT INTO incident.category_groups (code, name, icon, sort_order, is_active) VALUES
                                                                                   ('EVENT',     'Уведомления', 'event',     10, TRUE),
                                                                                   ('VIOLATION', 'Нарушения',   'violation', 20, TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET name       = EXCLUDED.name,
                              icon       = EXCLUDED.icon,
                              sort_order = EXCLUDED.sort_order,
                              is_active  = EXCLUDED.is_active,
                              updated_at = now();

-- -----------------------------------------------------------------------------
-- 2. Категории
-- -----------------------------------------------------------------------------
INSERT INTO incident.violation_categories
(group_id, code, name, description, sort_order, is_active)
VALUES
    ((SELECT id FROM incident.category_groups WHERE code = 'EVENT'),
     'EVENT_GENERAL',
     'Общая',
     'Общие уведомления: технические, инфраструктурные, нарушения регламента',
     10, TRUE),

    ((SELECT id FROM incident.category_groups WHERE code = 'VIOLATION'),
     'VIOLATION_BIOSAFETY',
     'Биобезопасность',
     'Нарушения биобезопасности: санитарные обработки, транспортные операции',
     10, TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET group_id    = EXCLUDED.group_id,
                              name        = EXCLUDED.name,
                              description = EXCLUDED.description,
                              sort_order  = EXCLUDED.sort_order,
                              is_active   = EXCLUDED.is_active,
                              updated_at  = now();

-- -----------------------------------------------------------------------------
-- 3. Чек-листы для «Общая» (EVENT_GENERAL)
-- -----------------------------------------------------------------------------
INSERT INTO incident.checklist_items
(category_id, code, name, description, comment_template,
 default_severity, sort_order, is_active)
VALUES
    ((SELECT id FROM incident.violation_categories WHERE code = 'EVENT_GENERAL'),
     'TECHNICAL',
     'Технические',
     'Технические неисправности оборудования',
     'Технический сбой: ',
     'MEDIUM', 10, TRUE),

    ((SELECT id FROM incident.violation_categories WHERE code = 'EVENT_GENERAL'),
     'INFRASTRUCTURE',
     'Инфраструктурные',
     'Инфраструктурные инциденты: питание, сеть, связь',
     'Инфраструктурный инцидент: ',
     'MEDIUM', 20, TRUE),

    ((SELECT id FROM incident.violation_categories WHERE code = 'EVENT_GENERAL'),
     'REGULATION_VIOLATIONS',
     'Нарушения регламента',
     'Нарушения внутреннего регламента и инструкций',
     'Нарушение регламента: ',
     'HIGH', 30, TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET name             = EXCLUDED.name,
                              description      = EXCLUDED.description,
                              comment_template = EXCLUDED.comment_template,
                              default_severity = EXCLUDED.default_severity,
                              sort_order       = EXCLUDED.sort_order,
                              is_active        = EXCLUDED.is_active,
                              updated_at       = now();

-- -----------------------------------------------------------------------------
-- 4. Чек-листы для «Биобезопасность» (VIOLATION_BIOSAFETY)
-- -----------------------------------------------------------------------------
INSERT INTO incident.checklist_items
(category_id, code, name, description, comment_template,
 default_severity, sort_order, is_active)
VALUES
    ((SELECT id FROM incident.violation_categories WHERE code = 'VIOLATION_BIOSAFETY'),
     'SANITARY_TREATMENTS',
     'Санитарные обработки',
     'Дератизация, дезинфекция, замена дезбарьеров',
     'Санитарная обработка: ',
     'MEDIUM', 10, TRUE),

    ((SELECT id FROM incident.violation_categories WHERE code = 'VIOLATION_BIOSAFETY'),
     'TRANSPORT_WEIGHT_DISCREPANCY',
     'Транспорт: расхождение по весу',
     'Положительное/отрицательное расхождение веса после выгрузки корма',
     'Расхождение по весу: ',
     'HIGH', 20, TRUE),

    ((SELECT id FROM incident.violation_categories WHERE code = 'VIOLATION_BIOSAFETY'),
     'TRANSPORT_OPERATIONS',
     'Транспортные операции',
     'Погрузка/выгрузка, животновозы, кормовозы, спермопродукция',
     'Транспортная операция: ',
     'MEDIUM', 30, TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET name             = EXCLUDED.name,
                              description      = EXCLUDED.description,
                              comment_template = EXCLUDED.comment_template,
                              default_severity = EXCLUDED.default_severity,
                              sort_order       = EXCLUDED.sort_order,
                              is_active        = EXCLUDED.is_active,
                              updated_at       = now();

COMMIT;

-- =============================================================================
-- Проверка (выполнить вручную после COMMIT):
--
-- SELECT g.sort_order AS g_ord, g.code AS group_code, g.name AS group_name,
--        c.sort_order AS c_ord, c.code AS category_code, c.name AS category_name,
--        i.sort_order AS i_ord, i.code AS item_code, i.name AS item_name,
--        i.default_severity
-- FROM   incident.category_groups       g
--        LEFT JOIN incident.violation_categories c ON c.group_id = g.id
--        LEFT JOIN incident.checklist_items     i ON i.category_id = c.id
-- WHERE  g.is_active AND c.is_active AND i.is_active
-- ORDER  BY g.sort_order, c.sort_order, i.sort_order;
-- =============================================================================