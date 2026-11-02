-- =============================================================================
-- 010_seed_ref_dictionaries.sql
-- -----------------------------------------------------------------------------
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