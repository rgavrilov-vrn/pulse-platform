-- =============================================================================
-- 007_create_schema_incident.sql
-- Чистое создание схемы incident и начальных справочников.
-- Предусловия: схема incident отсутствует; auth.users уже создана;
-- функция gen_random_uuid() доступна в используемой версии PostgreSQL.
-- Скрипт не удаляет существующие объекты и не предназначен для повторного запуска.
-- Все DDL и seed выполняются одной транзакцией.
-- Инцидент не содержит доменных полей места, камеры и сотрудника:
-- доменные связи хранятся в context_refs, расширяемые атрибуты — в metadata.
-- =============================================================================

BEGIN;

CREATE SCHEMA incident;

-- 1. Источники и справочники
CREATE TABLE incident.sources (
                                  code VARCHAR(64) PRIMARY KEY,
                                  name VARCHAR(255) NOT NULL,
                                  is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incident.category_groups (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          code VARCHAR(100) NOT NULL UNIQUE,
                                          name VARCHAR(255) NOT NULL,
                                          icon VARCHAR(100),
                                          sort_order INTEGER NOT NULL DEFAULT 0,
                                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                          updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incident.violation_categories (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               group_id UUID NOT NULL REFERENCES incident.category_groups(id),
                                               code VARCHAR(100) NOT NULL UNIQUE,
                                               name VARCHAR(255) NOT NULL,
                                               description TEXT,
                                               sort_order INTEGER NOT NULL DEFAULT 0,
                                               is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                               created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                               updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incident.checklist_items (
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

CREATE TABLE incident.status_transitions (
                                             from_status VARCHAR(32) NOT NULL,
                                             to_status VARCHAR(32) NOT NULL,
                                             PRIMARY KEY (from_status, to_status),
                                             CONSTRAINT chk_status_transitions_from CHECK (from_status IN
                                                                                           ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED')),
                                             CONSTRAINT chk_status_transitions_to CHECK (to_status IN
                                                                                         ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED'))
);
-- Матрица переходов заполняется отдельным существующим скриптом 008_1.

-- 2. Инциденты и нумерация
CREATE SEQUENCE incident.incident_number_seq START WITH 1 INCREMENT BY 1;

CREATE FUNCTION incident.generate_incident_number()
    RETURNS VARCHAR LANGUAGE plpgsql AS $$
DECLARE
next_number BIGINT;
BEGIN
    next_number := nextval('incident.incident_number_seq');
RETURN 'INC-' || to_char(current_date, 'YYYY') || '-'
    || lpad(next_number::TEXT, 6, '0');
END;
$$;

CREATE TABLE incident.incidents (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    number VARCHAR(30) NOT NULL UNIQUE,
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
                                    CONSTRAINT chk_incidents_version CHECK (version >= 0),
                                    CONSTRAINT chk_incidents_dates CHECK (updated_at >= created_at),
                                    CONSTRAINT chk_incidents_due_date CHECK (due_at IS NULL OR due_at >= created_at),
                                    CONSTRAINT chk_incidents_creation_mode CHECK (creation_mode IN ('INTEGRATION','MANUAL','IMPORT')),
                                    CONSTRAINT chk_incidents_status CHECK (status IN
                                                                           ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED')),
                                    CONSTRAINT chk_incidents_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE FUNCTION incident.set_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
RETURN NEW;
END;
$$;

CREATE FUNCTION incident.set_incident_number()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.number IS NULL OR NEW.number = '' THEN
        NEW.number := incident.generate_incident_number();
END IF;
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_incidents_updated_at
    BEFORE UPDATE ON incident.incidents
    FOR EACH ROW EXECUTE FUNCTION incident.set_updated_at();

CREATE TRIGGER trg_set_incident_number
    BEFORE INSERT ON incident.incidents
    FOR EACH ROW EXECUTE FUNCTION incident.set_incident_number();

-- 3. Контекст и пункты чек-листа события
CREATE TABLE incident.context_refs (
                                       incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE CASCADE,
                                       domain VARCHAR(64) NOT NULL,
                                       entity_type VARCHAR(64) NOT NULL,
                                       entity_id UUID NOT NULL,
                                       is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                       entity_name_snapshot VARCHAR(255),
                                       metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       PRIMARY KEY (incident_id, domain, entity_type, entity_id),
                                       CONSTRAINT chk_context_refs_domain CHECK (domain ~ '^[a-z][a-z0-9_]*$'),
    CONSTRAINT chk_context_refs_entity_type CHECK (entity_type ~ '^[a-z][a-z0-9_]*$')
);

-- Сразу окончательная структура: UUID PK, soft delete, без legacy checkpoint.
CREATE TABLE incident.incident_checklist_items (
                                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                   incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
                                                   checklist_item_id UUID NOT NULL REFERENCES incident.checklist_items(id) ON DELETE RESTRICT,
                                                   item_name_snapshot VARCHAR(500) NOT NULL,
                                                   category_name_snapshot VARCHAR(255),
                                                   severity VARCHAR(32) NOT NULL,
                                                   comment TEXT,
                                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                   created_by UUID NOT NULL REFERENCES auth.users(id),
                                                   is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                                   deleted_at TIMESTAMPTZ,
                                                   deleted_by UUID REFERENCES auth.users(id),
                                                   CONSTRAINT chk_incident_checklist_items_severity
                                                       CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

-- 4. Входящие события
CREATE TABLE incident.source_events (
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
                                        CONSTRAINT chk_source_events_status CHECK (processing_status IN ('RECEIVED','PROCESSED','FAILED','DUPLICATE')),
                                        CONSTRAINT chk_source_events_retry_count CHECK (retry_count >= 0)
);

-- 5. Материалы и архив
CREATE TABLE incident.media_assets (
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
                                       CONSTRAINT chk_media_size CHECK (size_bytes >= 0),
                                       CONSTRAINT chk_media_sha256 CHECK (sha256 ~ '^[0-9a-fA-F]{64}$'),
    CONSTRAINT chk_media_material_type CHECK (material_type IN ('IMAGE','VIDEO','DOCUMENT','OTHER'))
);

CREATE TABLE incident.incident_materials (
                                             incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
                                             media_asset_id UUID NOT NULL REFERENCES incident.media_assets(id) ON DELETE RESTRICT,
                                             role VARCHAR(32) NOT NULL,
                                             sort_order INTEGER NOT NULL DEFAULT 0,
                                             description TEXT,
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             created_by UUID NOT NULL REFERENCES auth.users(id),
                                             PRIMARY KEY (incident_id, media_asset_id),
                                             CONSTRAINT chk_incident_materials_role CHECK (role IN ('PRIMARY_EVIDENCE','ADDITIONAL_EVIDENCE','ARCHIVE_FRAGMENT','REPORT_ATTACHMENT'))
);

CREATE TABLE incident.archive_references (
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
                                             CONSTRAINT chk_archive_reference_type CHECK (reference_type IN ('URL','NATIVE_LINK','EVENT_ID','FILE')),
                                             CONSTRAINT chk_archive_reference_period CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)
);

-- 6. Комментарии, назначения, история
CREATE TABLE incident.comments (
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
                                   CONSTRAINT chk_comments_type CHECK (comment_type IN ('OPERATOR','REVIEW','RESOLUTION','SYSTEM'))
);

CREATE TABLE incident.assignments (
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

CREATE TABLE incident.status_history (
                                         id BIGSERIAL PRIMARY KEY,
                                         incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
                                         from_status VARCHAR(32),
                                         to_status VARCHAR(32) NOT NULL,
                                         changed_by UUID NOT NULL REFERENCES auth.users(id),
                                         changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         comment TEXT,
                                         request_id VARCHAR(100),
                                         metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
                                         CONSTRAINT chk_status_history_from CHECK (from_status IS NULL OR from_status IN
                                                                                                          ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED')),
                                         CONSTRAINT chk_status_history_to CHECK (to_status IN
                                                                                 ('DRAFT','RECORDED','NEEDS_REVIEW','CONFIRMED','REJECTED','CLOSED','CANCELLED','ARCHIVED'))
);

-- 7. Индексы
CREATE UNIQUE INDEX ux_context_refs_primary ON incident.context_refs (incident_id, domain) WHERE is_primary;
CREATE INDEX ix_context_refs_entity ON incident.context_refs (domain, entity_type, entity_id);
CREATE INDEX ix_context_refs_metadata_gin ON incident.context_refs USING GIN (metadata);
CREATE INDEX ix_violation_categories_group ON incident.violation_categories (group_id, sort_order);
CREATE INDEX ix_checklist_items_category ON incident.checklist_items (category_id, sort_order) WHERE is_active;
CREATE UNIQUE INDEX ux_incident_checklist_items_active
    ON incident.incident_checklist_items (incident_id, checklist_item_id) WHERE NOT is_deleted;
CREATE INDEX ix_incident_checklist_items_incident
    ON incident.incident_checklist_items (incident_id, created_at DESC) WHERE NOT is_deleted;
CREATE UNIQUE INDEX ux_source_events_external_id
    ON incident.source_events (source, external_event_id) WHERE external_event_id IS NOT NULL;
CREATE UNIQUE INDEX ux_source_events_idempotency_key
    ON incident.source_events (source, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ix_source_events_payload_gin ON incident.source_events USING GIN (payload);
CREATE INDEX ix_incidents_event_time ON incident.incidents (event_time DESC);
CREATE INDEX ix_incidents_created_at ON incident.incidents (created_at DESC);
CREATE INDEX ix_incidents_status ON incident.incidents (status);
CREATE INDEX ix_incidents_source ON incident.incidents (source);
CREATE INDEX ix_incidents_operator ON incident.incidents (operator_id);
CREATE INDEX ix_incidents_assigned_to ON incident.incidents (assigned_to);
CREATE INDEX ix_incidents_severity_status ON incident.incidents (severity, status);
CREATE INDEX ix_incidents_status_event_time ON incident.incidents (status, event_time DESC);
CREATE INDEX ix_incidents_metadata_gin ON incident.incidents USING GIN (metadata);
CREATE INDEX ix_incident_materials_incident ON incident.incident_materials (incident_id);
CREATE INDEX ix_status_history_incident ON incident.status_history (incident_id, changed_at DESC);
CREATE INDEX ix_comments_incident ON incident.comments (incident_id, created_at DESC);
CREATE UNIQUE INDEX ux_current_incident_assignment ON incident.assignments (incident_id) WHERE is_current;

-- 8. Реестр: удалённые привязки не учитываются в checklist_count.
CREATE VIEW incident.incident_list AS
SELECT
    i.id, i.number, i.source, i.creation_mode, i.status, i.severity,
    i.event_time, i.created_at, i.updated_at,
    operator_user.display_name AS operator_name,
    assigned_user.display_name AS assigned_to_name,
    (SELECT count(*) FROM incident.incident_materials im WHERE im.incident_id = i.id) AS material_count,
    (SELECT count(*) FROM incident.incident_checklist_items ici
     WHERE ici.incident_id = i.id AND NOT ici.is_deleted) AS checklist_count,
    (SELECT jsonb_object_agg(cr.domain, jsonb_build_object(
            'entityType', cr.entity_type, 'entityId', cr.entity_id,
            'entityName', cr.entity_name_snapshot))
     FROM incident.context_refs cr WHERE cr.incident_id = i.id AND cr.is_primary) AS primary_context
FROM incident.incidents i
         LEFT JOIN auth.users operator_user ON operator_user.id = i.operator_id
         LEFT JOIN auth.users assigned_user ON assigned_user.id = i.assigned_to;

-- 9. Начальные данные
INSERT INTO incident.sources (code, name) VALUES
                                              ('videoserver', 'Video Server'), ('manual', 'Manual');

INSERT INTO incident.category_groups (code, name, icon, sort_order) VALUES
                                                                        ('EVENT', 'Событие', 'event', 10),
                                                                        ('VIOLATION', 'Нарушение', 'violation', 20);

INSERT INTO incident.violation_categories (group_id, code, name, description, sort_order)
SELECT g.id, seed.code, seed.name, seed.description, seed.sort_order
FROM (VALUES
          ('EVENT', 'EVENT_GENERAL', 'Общая',
           'Общие события: технические, инфраструктурные, нарушения регламента', 10),
          ('VIOLATION', 'VIOLATION_BIOSAFETY', 'Биобезопасность',
           'Контроль биобезопасности: персонал, транспорт, санитарная обработка и перемещение ТМЦ', 10)
     ) AS seed(group_code, code, name, description, sort_order)
         JOIN incident.category_groups g ON g.code = seed.group_code;

-- Сохраняем шесть исходных кодов; добавляем 15 типов КТ из документа.
-- MEDIUM для новых типов — техническое значение по умолчанию, не оценка риска.
-- Новые шаблоны комментария и требования к материалам не задаются:
-- документ не содержит таких настроек.
INSERT INTO incident.checklist_items
(category_id, code, name, description, comment_template, default_severity, sort_order)
SELECT c.id, seed.code, seed.name, seed.description, seed.comment_template,
       seed.default_severity, seed.sort_order
FROM (VALUES
          ('EVENT_GENERAL', 'TECHNICAL', 'Технические', 'Технические неисправности оборудования', 'Технический сбой: ', 'MEDIUM', 10),
          ('EVENT_GENERAL', 'INFRASTRUCTURE', 'Инфраструктурные', 'Инфраструктурные инциденты: питание, сеть, связь', 'Инфраструктурный инцидент: ', 'MEDIUM', 20),
          ('EVENT_GENERAL', 'REGULATION_VIOLATIONS', 'Нарушения регламента', 'Нарушения внутреннего регламента и инструкций', 'Нарушение регламента: ', 'HIGH', 30),
          ('VIOLATION_BIOSAFETY', 'SANITARY_TREATMENTS', 'Санитарные обработки', 'Дератизация, дезинфекция, замена дезбарьеров', 'Санитарная обработка: ', 'MEDIUM', 10),
          ('VIOLATION_BIOSAFETY', 'TRANSPORT_WEIGHT_DISCREPANCY', 'Транспорт: расхождение по весу', 'Положительное/отрицательное расхождение веса после выгрузки корма', 'Расхождение по весу: ', 'HIGH', 20),
          ('VIOLATION_BIOSAFETY', 'TRANSPORT_OPERATIONS', 'Транспортные операции', 'Погрузка/выгрузка, животновозы, кормовозы, спермопродукция', 'Транспортная операция: ', 'MEDIUM', 30),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_ENTRY', 'Въезд', 'Контроль за осмотром товарных скотовозов, контроль прохода сотрудников на предприятие, контроль по передаче ТМЦ.', NULL, 'MEDIUM', 40),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_PREWASH', 'Предварительная мойка', 'Контроль качества помывки любого въезжающего автотранспорта на территорию СК.', NULL, 'MEDIUM', 50),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_MAIN_WASH', 'Основная мойка', 'Контроль качества помывки автотранспорта, соблюдение требований регламента ББ.', NULL, 'MEDIUM', 60),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_DISINFECTION_BARRIER', 'Дезбарьер', 'Соблюдение требований регламента ББ при проезде через дезбарьер.', NULL, 'MEDIUM', 70),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_TRANSFER_RAMP', 'Перегрузочная рампа', 'Перегрузочная рампа, рампа сан/тех брака, накопитель: контроль процесса отгрузки животных.', NULL, 'MEDIUM', 80),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_DISINFECTION_WAGON', 'Дезвагон', 'Соблюдение требований регламента ББ по передаче ТМЦ в серую и чистую зоны.', NULL, 'MEDIUM', 90),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_TRANSFER_WINDOW', 'Передаточное окно', 'Контроль передачи ТМЦ, обедов и семени через передаточное окно.', NULL, 'MEDIUM', 100),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_QUARTZ_ROOMS', 'Кварцевые комнаты', 'Контроль требований регламента ББ по передаче ТМЦ, семени, обедов в чистую зону.', NULL, 'MEDIUM', 110),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_FIRST_SANITARY_CHECKPOINT', 'Первый санпропускник', 'Контроль прохода сотрудников и посетителей через первый санпропускник, соблюдение требований регламента ББ.', NULL, 'MEDIUM', 120),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_HAND_PHOTO_CAPTURE', 'Система фотофиксации рук', 'Контроль прохода сотрудников и посетителей через систему фотофиксации рук.', NULL, 'MEDIUM', 130),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_SECOND_SANITARY_CHECKPOINT', 'Второй санпропускник', 'Контроль соблюдения требования по проходу сотрудниками и посетителями через второй санпропускник, согласно требованиям ББ.', NULL, 'MEDIUM', 140),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_CLEAN_ZONE_HYGIENE', 'Станция гигиены в чистой зоне', 'Контроль соблюдения требований по проходу сотрудников и посетителей на производство, согласно регламенту ББ.', NULL, 'MEDIUM', 150),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_RAMPS', 'Рампы', 'Контроль отгрузки животных, контроль передачи ТМЦ на чистую зону, помывка и дезинфекция рамп согласно требованиям ББ.', NULL, 'MEDIUM', 160),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_GREY_ZONE', 'Серая зона', 'Контроль процесса отгрузки кормовозов и скотовозов, проверка отсутствия открытого доступа на чистую зону (верхние люки бункеров, разгрузочные камлоки, пожарные выходы), целостность ограждения.', NULL, 'MEDIUM', 170),
          ('VIOLATION_BIOSAFETY', 'BIOSECURITY_MORTALITY_DISPOSAL', 'Трупник, крематор', 'Соблюдение требований по вывозу падежа, согласно регламенту ББ.', NULL, 'MEDIUM', 180)
     ) AS seed(category_code, code, name, description, comment_template, default_severity, sort_order)
         JOIN incident.violation_categories c ON c.code = seed.category_code;

-- 10. Комментарии к итоговой модели
COMMENT ON SCHEMA incident IS 'Инциденты: доменно-нейтральная сущность и собственные справочники.';
COMMENT ON TABLE incident.sources IS 'Источники инцидентов.';
COMMENT ON TABLE incident.category_groups IS 'Группы категорий: Событие / Нарушение.';
COMMENT ON COLUMN incident.category_groups.icon IS 'Имя иконки для UI (optional).';
COMMENT ON TABLE incident.violation_categories IS 'Категории событий и нарушений.';
COMMENT ON COLUMN incident.violation_categories.code IS 'Уникальный код категории.';
COMMENT ON COLUMN incident.violation_categories.group_id IS 'Группа категории.';
COMMENT ON COLUMN incident.violation_categories.sort_order IS 'Порядок сортировки в UI.';
COMMENT ON TABLE incident.checklist_items IS 'Справочник пунктов чек-листа (типов контрольных точек).';
COMMENT ON COLUMN incident.checklist_items.code IS 'Уникальный стабильный код пункта.';
COMMENT ON COLUMN incident.checklist_items.category_id IS 'Категория пункта чек-листа.';
COMMENT ON COLUMN incident.checklist_items.default_severity IS 'Критичность по умолчанию.';
COMMENT ON COLUMN incident.checklist_items.comment_template IS 'Шаблон комментария по умолчанию.';
COMMENT ON TABLE incident.incidents IS 'Инциденты. Доменные привязки находятся в context_refs.';
COMMENT ON COLUMN incident.incidents.number IS 'Человекочитаемый номер INC-YYYY-NNNNNN, назначается триггером.';
COMMENT ON COLUMN incident.incidents.version IS 'Версия для оптимистичной блокировки; начальное значение 0.';
COMMENT ON COLUMN incident.incidents.event_time IS 'Время события.';
COMMENT ON COLUMN incident.incidents.created_at IS 'Время регистрации.';
COMMENT ON COLUMN incident.incidents.metadata IS 'Расширяемые атрибуты инцидента (JSONB).';
COMMENT ON TABLE incident.context_refs IS 'Доменно-нейтральные ссылки на камеру, объект и другие сущности.';
COMMENT ON COLUMN incident.context_refs.entity_id IS 'ID сущности; межсхемный FK намеренно не установлен.';
COMMENT ON COLUMN incident.context_refs.is_primary IS 'Основная ссылка в домене: не более одной для инцидента.';
COMMENT ON COLUMN incident.context_refs.entity_name_snapshot IS 'Имя сущности на момент привязки.';
COMMENT ON TABLE incident.incident_checklist_items IS 'Привязки пунктов чек-листа со снимками имён и мягким удалением.';
COMMENT ON COLUMN incident.incident_checklist_items.id IS 'UUID первичный ключ.';
COMMENT ON COLUMN incident.incident_checklist_items.is_deleted IS 'Флаг мягкого удаления.';
COMMENT ON COLUMN incident.incident_checklist_items.deleted_at IS 'Время мягкого удаления.';
COMMENT ON COLUMN incident.incident_checklist_items.deleted_by IS 'Кто выполнил мягкое удаление.';
COMMENT ON COLUMN incident.incident_checklist_items.item_name_snapshot IS 'Имя пункта на момент привязки.';
COMMENT ON COLUMN incident.incident_checklist_items.category_name_snapshot IS 'Имя категории на момент привязки.';
COMMENT ON TABLE incident.source_events IS 'Входящие события источников и результаты обработки.';
COMMENT ON COLUMN incident.source_events.external_event_id IS 'Натуральный ID события во внешней системе для дедупликации.';
COMMENT ON COLUMN incident.source_events.idempotency_key IS 'Клиентский ключ идемпотентности: альтернативный ключ дедупликации.';
COMMENT ON TABLE incident.status_transitions IS 'Матрица разрешённых переходов статусов.';
COMMENT ON TABLE incident.media_assets IS 'Медиа-активы в файловом или объектном хранилище.';
COMMENT ON COLUMN incident.media_assets.sha256 IS 'SHA-256 файла: 64 шестнадцатеричных символа.';
COMMENT ON TABLE incident.incident_materials IS 'Связь инцидента с материалами.';
COMMENT ON TABLE incident.archive_references IS 'Ссылки на архивные материалы.';
COMMENT ON TABLE incident.comments IS 'Комментарии с мягким удалением.';
COMMENT ON TABLE incident.assignments IS 'История назначений; не более одного текущего назначения.';
COMMENT ON TABLE incident.status_history IS 'История переходов статусов.';

COMMIT;

-- Проверка после успешного COMMIT: 2 группы, 2 категории, 21 пункт.
-- EVENT_GENERAL: 3; VIOLATION_BIOSAFETY: 18 (3 исходных + 15 из документа).
-- SELECT g.code AS group_code, c.code AS category_code, count(i.id) AS item_count
-- FROM incident.category_groups g
-- JOIN incident.violation_categories c ON c.group_id = g.id
-- LEFT JOIN incident.checklist_items i ON i.category_id = c.id AND i.is_active
-- GROUP BY g.code, c.code ORDER BY g.code, c.code;
