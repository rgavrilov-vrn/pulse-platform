-- =============================================================================
-- 006_create_schema_ref.sql
-- -----------------------------------------------------------------------------
-- Справочные данные (reference data): объекты, зоны, подразделения, сотрудники.
--
-- ВАЖНО:
--  * ref.video_servers / ref.cameras — перенесены в videoserver.* (007).
--  * ref.violation_categories / ref.checklist_items / ref.status_transitions —
--    перенесены в incident.* (002): это справочники домена инцидентов.
--  * ref.object_checklist_items — удалена (доменное понятие видео-домена).
--
-- Схема ref не зависит от incident.* и videoserver.* — полностью автономна.
-- Порядок миграций: 001 → 002 → 006 → 007.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ref;

CREATE TABLE IF NOT EXISTS ref.objects (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS ref.zones (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    object_id UUID NOT NULL REFERENCES ref.objects(id),
    parent_id UUID REFERENCES ref.zones(id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_zones_object_code UNIQUE (object_id, code)
    );

CREATE TABLE IF NOT EXISTS ref.departments (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS ref.employees (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    personnel_no VARCHAR(100) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    department_id UUID REFERENCES ref.departments(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- -----------------------------------------------------------------------------
-- Комментарии
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA ref IS 'Справочные данные: объекты, зоны, подразделения, сотрудники. Автономна.';

COMMENT ON TABLE  ref.objects IS 'Объекты (площадки, здания, цеха). Корень пространственной иерархии.';
COMMENT ON COLUMN ref.objects.code      IS 'Внутренний код объекта';
COMMENT ON COLUMN ref.objects.address   IS 'Физический адрес';
COMMENT ON COLUMN ref.objects.is_active IS 'Объект активен';

COMMENT ON TABLE  ref.zones IS 'Зоны внутри объектов (иерархические через parent_id).';
COMMENT ON COLUMN ref.zones.object_id IS 'Родительский объект';
COMMENT ON COLUMN ref.zones.parent_id IS 'Родительская зона (null — корневая зона объекта)';
COMMENT ON COLUMN ref.zones.code      IS 'Код зоны (уникален в пределах объекта)';

COMMENT ON TABLE  ref.departments IS 'Организационные подразделения.';
COMMENT ON TABLE  ref.employees   IS 'Сотрудники.';
COMMENT ON COLUMN ref.employees.personnel_no  IS 'Табельный номер (уникален)';
COMMENT ON COLUMN ref.employees.department_id IS 'Подразделение';

-- =============================================================================
-- 010_seed_ref_dictionaries.sql
-- Seed справочников ref.*:
--   - 7 площадок (из выгрузки отчётов операторов от 01.10.2026)
--   - 7 типовых зон × каждая площадка
--   - 3 подразделения: Ситуационный центр, Биобезопасность, Информационная безопасность
--   - сотрудники в каждом подразделении
--
-- Идемпотентно: ON CONFLICT (...) DO UPDATE — повторный прогон не создаёт
-- дублей, id сущностей не меняются, правки текста подхватываются.
--
-- Зависит от: 006_create_schema_refs.sql
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Площадки (ref.objects)
-- -----------------------------------------------------------------------------
INSERT INTO ref.objects (code, name, description, address, is_active) VALUES
                                                                          ('MEHETSKY',      'Мечетский СК',          'Свинокомплекс Мечетский',         'Ростовская обл., с. Мечетка',       TRUE),
                                                                          ('NOVOHOPERSKY',  'Новохоперский СК',      'Свинокомплекс Новохоперский',     'Воронежская обл., г. Новохоперск',  TRUE),
                                                                          ('TROITSKY',      'Троицкий СК',           'Свинокомплекс Троицкий',          'Воронежская обл., с. Троицкое',     TRUE),
                                                                          ('MELOVATSKY',    'Меловатский СК',        'Свинокомплекс Меловатский',       'Воронежская обл., с. Меловатка',    TRUE),
                                                                          ('OKTYABRSKY',    'Октябрьский СК',        'Свинокомплекс Октябрьский',       'Воронежская обл., п. Октябрьский',  TRUE),
                                                                          ('BEREZOVSKY',    'Березовский СК',        'Свинокомплекс Березовский',       'Воронежская обл., с. Березовка',    TRUE),
                                                                          ('GAVRILSKY',     'Гаврильский СК',        'Свинокомплекс Гаврильский',       'Воронежская обл., с. Гаврильск',    TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET name        = EXCLUDED.name,
                              description = EXCLUDED.description,
                              address     = EXCLUDED.address,
                              is_active   = EXCLUDED.is_active,
                              updated_at  = now();

-- -----------------------------------------------------------------------------
-- 2. Зоны (ref.zones) — типовой набор на каждую площадку.
--    Кросс-джойн с шаблонами: одна строка шаблона → одна зона на каждой площадке.
-- -----------------------------------------------------------------------------
WITH zone_templates(code, name, description) AS (
    VALUES
        ('CLEAN',      'Чистая зона',          'Зона с наивысшими требованиями биобезопасности'),
        ('GRAY',       'Серая зона',           'Переходная зона между чистой и технической'),
        ('TECHNICAL',  'Техническая зона',     'Зона технического обслуживания'),
        ('RAMP',       'Перегрузочная рампа',  'Зона погрузки/выгрузки животных и продукции'),
        ('MPP',        'МПП',                  'Мясоперерабатывающее производство'),
        ('DEZBARRIER', 'Дезбарьер',            'Дезинфекционный барьер на въезде/выезде'),
        ('TRUPNIK',    'Трупник',              'Место временного хранения биологических отходов')
)
INSERT INTO ref.zones (object_id, code, name, description)
SELECT o.id, t.code, t.name, t.description
FROM ref.objects o
         CROSS JOIN zone_templates t
WHERE o.is_active
    ON CONFLICT (object_id, code) DO UPDATE
                                         SET name        = EXCLUDED.name,
                                         description = EXCLUDED.description,
                                         updated_at  = now();

-- -----------------------------------------------------------------------------
-- 3. Подразделения (ref.departments)
-- -----------------------------------------------------------------------------
INSERT INTO ref.departments (code, name, is_active) VALUES
                                                        ('SITCENTER', 'Ситуационный центр',        TRUE),
                                                        ('BIOSEC',    'Биобезопасность',           TRUE),
                                                        ('INFOSEC',   'Информационная безопасность', TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET name      = EXCLUDED.name,
                              is_active = EXCLUDED.is_active,
                              updated_at = now();

-- -----------------------------------------------------------------------------
-- 4. Сотрудники (ref.employees)
--    personnel_no уникален; department_id через подзапрос по code.
-- -----------------------------------------------------------------------------
WITH emp(personnel_no, full_name, dept_code) AS (
    VALUES
        -- Ситуационный центр
        ('SC-001', 'Иванов Иван Иванович',       'SITCENTER'),
        ('SC-002', 'Петров Пётр Петрович',       'SITCENTER'),
        ('SC-003', 'Сидоров Сергей Сергеевич',   'SITCENTER'),

        -- Биобезопасность
        ('BS-001', 'Смирнов Сергей Сергеевич',   'BIOSEC'),
        ('BS-002', 'Кузнецов Кузьма Кузьмич',    'BIOSEC'),
        ('BS-003', 'Морозова Мария Михайловна',  'BIOSEC'),

        -- Информационная безопасность
        ('IS-001', 'Попов Павел Павлович',       'INFOSEC'),
        ('IS-002', 'Соколов Степан Степанович',  'INFOSEC')
)
INSERT INTO ref.employees (personnel_no, full_name, department_id, is_active)
SELECT e.personnel_no,
       e.full_name,
       (SELECT id FROM ref.departments WHERE code = e.dept_code),
       TRUE
FROM emp e
    ON CONFLICT (personnel_no) DO UPDATE
                                      SET full_name     = EXCLUDED.full_name,
                                      department_id = EXCLUDED.department_id,
                                      is_active     = EXCLUDED.is_active,
                                      updated_at    = now();

COMMIT;

-- =============================================================================
-- Проверка (вручную после COMMIT)
-- =============================================================================
-- Площадки и число зон:
-- SELECT o.code, o.name, COUNT(z.id) AS zones
-- FROM ref.objects o
-- LEFT JOIN ref.zones z ON z.object_id = o.id AND z.is_active
-- WHERE o.is_active
-- GROUP BY o.id, o.code, o.name
-- ORDER BY o.code;
--   ожидаем: 7 площадок × 7 зон = 49

-- Подразделения и сотрудники:
-- SELECT d.code, d.name, COUNT(e.id) AS employees
-- FROM ref.departments d
-- LEFT JOIN ref.employees e ON e.department_id = d.id AND e.is_active
-- WHERE d.is_active
-- GROUP BY d.id, d.code, d.name
-- ORDER BY d.code;
--   ожидаем: SITCENTER=3, BIOSEC=3, INFOSEC=2