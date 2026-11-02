-- 008_1_seed_status_transitions.sql
-- Базовая матрица разрешённых переходов статусов инцидента.
-- ADMIN может расширить через отдельный endpoint (в будущем) или миграцию.

INSERT INTO incident.status_transitions (from_status, to_status) VALUES
        ('DRAFT',         'RECORDED'),
        ('DRAFT',         'CANCELLED'),
        ('RECORDED',      'NEEDS_REVIEW'),
        ('RECORDED',      'CONFIRMED'),
        ('RECORDED',      'REJECTED'),
        ('RECORDED',      'CANCELLED'),
        ('NEEDS_REVIEW',  'CONFIRMED'),
        ('NEEDS_REVIEW',  'REJECTED'),
        ('NEEDS_REVIEW',  'CANCELLED'),
        ('CONFIRMED',     'CLOSED'),
        ('CONFIRMED',     'CANCELLED'),
        ('REJECTED',      'ARCHIVED'),
        ('CLOSED',        'ARCHIVED')
ON CONFLICT (from_status, to_status) DO NOTHING;

COMMENT ON TABLE incident.status_transitions IS
    'Матрица разрешённых переходов статусов (seed 008_1). Пустая таблица = запрет всех переходов.';