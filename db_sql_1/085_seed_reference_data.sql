-- 085_seed_reference_data.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

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
INSERT INTO ref.departments (code, name, is_active) VALUES
                                                        ('SITCENTER', 'Ситуационный центр',        TRUE),
                                                        ('BIOSEC',    'Биобезопасность',           TRUE),
                                                        ('INFOSEC',   'Информационная безопасность', TRUE)
    ON CONFLICT (code) DO UPDATE
                              SET name      = EXCLUDED.name,
                              is_active = EXCLUDED.is_active,
                              updated_at = now();

COMMIT;
