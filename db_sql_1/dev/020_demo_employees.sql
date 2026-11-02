-- dev/020_demo_employees.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

WITH emp(personnel_no, full_name, dept_code) AS (
    VALUES
        ('SC-001', 'Иванов Иван Иванович',       'SITCENTER'),
        ('SC-002', 'Петров Пётр Петрович',       'SITCENTER'),
        ('SC-003', 'Сидоров Сергей Сергеевич',   'SITCENTER'),
        ('BS-001', 'Смирнов Сергей Сергеевич',   'BIOSEC'),
        ('BS-002', 'Кузнецов Кузьма Кузьмич',    'BIOSEC'),
        ('BS-003', 'Морозова Мария Михайловна',  'BIOSEC'),
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
