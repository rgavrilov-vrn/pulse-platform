-- V10__incident_seed.sql
-- Seed: источники, группы категорий, категории, чек-листы, матрица переходов.
-- Единственный источник правды для status_transitions (устранена находка #103).

SET search_path = pg_catalog, public;

INSERT INTO incident.sources (code, name) VALUES
                                              ('videoserver', 'Video Server'),
                                              ('manual',      'Manual')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO incident.category_groups (code, name, icon, sort_order) VALUES
                                                                        ('EVENT',     'Событие',  'event',     10),
                                                                        ('VIOLATION', 'Нарушение','violation', 20)
    ON CONFLICT (code) DO NOTHING;

INSERT INTO incident.violation_categories (group_id, code, name, description, sort_order)
SELECT g.id, s.code, s.name, s.description, s.sort_order
FROM (VALUES
          ('EVENT',     'EVENT_GENERAL',       'Общая',           'Общие события: технические, инфраструктурные, нарушения регламента', 10),
          ('VIOLATION', 'VIOLATION_BIOSAFETY', 'Биобезопасность', 'Контроль биобезопасности', 10)
     ) AS s(group_code, code, name, description, sort_order)
         JOIN incident.category_groups g ON g.code = s.group_code
    ON CONFLICT (code) DO NOTHING;

INSERT INTO incident.checklist_items
(category_id, code, name, description, comment_template, default_severity, sort_order)
SELECT c.id, s.code, s.name, s.description, s.comment_template, s.default_severity, s.sort_order
FROM (VALUES
          ('EVENT_GENERAL','TECHNICAL','Технические','Технические неисправности','Технический сбой: ','MEDIUM',10),
          ('EVENT_GENERAL','INFRASTRUCTURE','Инфраструктурные','Питание/сеть/связь','Инфраструктурный инцидент: ','MEDIUM',20),
          ('EVENT_GENERAL','REGULATION_VIOLATIONS','Нарушения регламента','Нарушения инструкций','Нарушение регламента: ','HIGH',30),
          ('VIOLATION_BIOSAFETY','SANITARY_TREATMENTS','Санитарные обработки','Дератизация, дезинфекция','Санитарная обработка: ','MEDIUM',10),
          ('VIOLATION_BIOSAFETY','TRANSPORT_WEIGHT_DISCREPANCY','Расхождение по весу','Расхождение веса после выгрузки','Расхождение по весу: ','HIGH',20),
          ('VIOLATION_BIOSAFETY','TRANSPORT_OPERATIONS','Транспортные операции','Погрузка/выгрузка','Транспортная операция: ','MEDIUM',30),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_ENTRY','Въезд','Контроль осмотра/прохода',NULL,'MEDIUM',40),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_PREWASH','Предварительная мойка','Помывка автотранспорта',NULL,'MEDIUM',50),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_MAIN_WASH','Основная мойка','Требования регламента ББ',NULL,'MEDIUM',60),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_DISINFECTION_BARRIER','Дезбарьер','Проезд через дезбарьер',NULL,'MEDIUM',70),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_TRANSFER_RAMP','Перегрузочная рампа','Отгрузка животных',NULL,'MEDIUM',80),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_DISINFECTION_WAGON','Дезвагон','Передача ТМЦ',NULL,'MEDIUM',90),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_TRANSFER_WINDOW','Передаточное окно','Передача ТМЦ/семени',NULL,'MEDIUM',100),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_QUARTZ_ROOMS','Кварцевые комнаты','Передача в чистую зону',NULL,'MEDIUM',110),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_FIRST_SANITARY_CHECKPOINT','Первый санпропускник','Проход сотрудников',NULL,'MEDIUM',120),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_HAND_PHOTO_CAPTURE','Фотофиксация рук','Система фотофиксации',NULL,'MEDIUM',130),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_SECOND_SANITARY_CHECKPOINT','Второй санпропускник','Проход сотрудников',NULL,'MEDIUM',140),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_CLEAN_ZONE_HYGIENE','Станция гигиены','Требования ББ',NULL,'MEDIUM',150),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_RAMPS','Рампы','Отгрузка/передача/мойка',NULL,'MEDIUM',160),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_GREY_ZONE','Серая зона','Контроль доступа',NULL,'MEDIUM',170),
          ('VIOLATION_BIOSAFETY','BIOSECURITY_MORTALITY_DISPOSAL','Трупник/крематор','Вывоз падежа',NULL,'MEDIUM',180)
     ) AS s(category_code, code, name, description, comment_template, default_severity, sort_order)
         JOIN incident.violation_categories c ON c.code = s.category_code
    ON CONFLICT (code) DO NOTHING;

-- Матрица переходов (единственный источник — здесь)
INSERT INTO incident.status_transitions (from_status, to_status) VALUES
                                                                     ('DRAFT',        'RECORDED'),
                                                                     ('DRAFT',        'CANCELLED'),
                                                                     ('RECORDED',     'NEEDS_REVIEW'),
                                                                     ('RECORDED',     'CONFIRMED'),
                                                                     ('RECORDED',     'REJECTED'),
                                                                     ('RECORDED',     'CANCELLED'),
                                                                     ('NEEDS_REVIEW', 'CONFIRMED'),
                                                                     ('NEEDS_REVIEW', 'REJECTED'),
                                                                     ('NEEDS_REVIEW', 'CANCELLED'),
                                                                     ('CONFIRMED',    'CLOSED'),
                                                                     ('CONFIRMED',    'CANCELLED'),
                                                                     ('REJECTED',     'DRAFT'),
                                                                     ('REJECTED',     'CANCELLED'),
                                                                     ('REJECTED',     'ARCHIVED'),
                                                                     ('CLOSED',       'ARCHIVED'),
                                                                     ('CANCELLED',    'ARCHIVED')
    ON CONFLICT (from_status, to_status) DO NOTHING;