-- 083_seed_incident_catalog.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

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

COMMIT;
