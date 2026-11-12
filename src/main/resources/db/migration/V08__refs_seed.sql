-- V08__refs_seed.sql
SET search_path = pg_catalog, public;

INSERT INTO ref.objects (code, name, description, address, is_active) VALUES
                                                                          ('MEHETSKY',     'Мечетский СК',     'Свинокомплекс Мечетский',     'Ростовская обл., с. Мечетка',      TRUE),
                                                                          ('NOVOHOPERSKY', 'Новохоперский СК', 'Свинокомплекс Новохоперский', 'Воронежская обл., г. Новохоперск', TRUE),
                                                                          ('TROITSKY',     'Троицкий СК',      'Свинокомплекс Троицкий',      'Воронежская обл., с. Троицкое',    TRUE),
                                                                          ('MELOVATSKY',   'Меловатский СК',   'Свинокомплекс Меловатский',   'Воронежская обл., с. Меловатка',   TRUE),
                                                                          ('OKTYABRSKY',   'Октябрьский СК',   'Свинокомплекс Октябрьский',   'Воронежская обл., п. Октябрьский', TRUE),
                                                                          ('BEREZOVSKY',   'Березовский СК',   'Свинокомплекс Березовский',   'Воронежская обл., с. Березовка',   TRUE),
                                                                          ('GAVRILSKY',    'Гаврильский СК',   'Свинокомплекс Гаврильский',   'Воронежская обл., с. Гаврильск',   TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET name = EXCLUDED.name, description = EXCLUDED.description,
                              address = EXCLUDED.address, is_active = EXCLUDED.is_active, updated_at = now();

WITH zone_templates(code, name, description) AS (VALUES
                                                     ('CLEAN',      'Чистая зона',         'Наивысшие требования биобезопасности'),
                                                     ('GRAY',       'Серая зона',          'Переходная зона'),
                                                     ('TECHNICAL',  'Техническая зона',    'Зона технического обслуживания'),
                                                     ('RAMP',       'Перегрузочная рампа', 'Погрузка/выгрузка'),
                                                     ('MPP',        'МПП',                 'Мясоперерабатывающее производство'),
                                                     ('DEZBARRIER', 'Дезбарьер',           'Дезинфекционный барьер'),
                                                     ('TRUPNIK',    'Трупник',             'Хранение биологических отходов')
)
INSERT INTO ref.zones (object_id, code, name, description)
SELECT o.id, t.code, t.name, t.description
FROM ref.objects o CROSS JOIN zone_templates t
WHERE o.is_active
    ON CONFLICT (object_id, code) DO UPDATE
                                         SET name = EXCLUDED.name, description = EXCLUDED.description, updated_at = now();

INSERT INTO ref.departments (code, name, is_active) VALUES
                                                        ('SITCENTER', 'Ситуационный центр',         TRUE),
                                                        ('BIOSEC',    'Биобезопасность',            TRUE),
                                                        ('INFOSEC',   'Информационная безопасность', TRUE)
    ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, is_active = EXCLUDED.is_active, updated_at = now();

WITH emp(personnel_no, full_name, dept_code) AS (VALUES
                                                     ('SC-001','Иванов Иван Иванович','SITCENTER'),
                                                     ('SC-002','Петров Пётр Петрович','SITCENTER'),
                                                     ('SC-003','Сидоров Сергей Сергеевич','SITCENTER'),
                                                     ('BS-001','Смирнов Сергей Сергеевич','BIOSEC'),
                                                     ('BS-002','Кузнецов Кузьма Кузьмич','BIOSEC'),
                                                     ('BS-003','Морозова Мария Михайловна','BIOSEC'),
                                                     ('IS-001','Попов Павел Павлович','INFOSEC'),
                                                     ('IS-002','Соколов Степан Степанович','INFOSEC')
)
INSERT INTO ref.employees (personnel_no, full_name, department_id, is_active)
SELECT e.personnel_no, e.full_name,
       (SELECT id FROM ref.departments WHERE code = e.dept_code), TRUE
FROM emp e
    ON CONFLICT (personnel_no) DO UPDATE
                                      SET full_name = EXCLUDED.full_name, department_id = EXCLUDED.department_id,
                                      is_active = EXCLUDED.is_active, updated_at = now();