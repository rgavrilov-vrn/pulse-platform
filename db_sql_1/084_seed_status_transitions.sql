-- 084_seed_status_transitions.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

INSERT INTO incident.status_transitions
(from_status, to_status)
VALUES
    ('DRAFT', 'RECORDED'),
    ('DRAFT', 'CANCELLED'),

    ('RECORDED', 'NEEDS_REVIEW'),
    ('RECORDED', 'CONFIRMED'),
    ('RECORDED', 'REJECTED'),
    ('RECORDED', 'CANCELLED'),

    ('NEEDS_REVIEW', 'CONFIRMED'),
    ('NEEDS_REVIEW', 'REJECTED'),
    ('NEEDS_REVIEW', 'CANCELLED'),

    ('CONFIRMED', 'CLOSED'),
    ('CONFIRMED', 'CANCELLED'),

    ('REJECTED', 'DRAFT'),
    ('REJECTED', 'CANCELLED'),

    ('CLOSED', 'ARCHIVED'),
    ('CANCELLED', 'ARCHIVED')

    ON CONFLICT DO NOTHING;

COMMIT;
