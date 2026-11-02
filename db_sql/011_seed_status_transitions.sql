-- =============================================================================
-- 011_seed_status_transitions.sql
-- -----------------------------------------------------------------------------
-- Базовая матрица разрешённых переходов статусов инцидента.
-- Единственный источник правды для incident.status_transitions.
-- ADMIN может расширить через отдельный endpoint (в будущем) или миграцию.
--
-- Зависит от: 007_create_schema_incident.sql
-- =============================================================================

INSERT INTO incident.status_transitions (from_status, to_status) VALUES
                                                                     -- DRAFT
                                                                     ('DRAFT',         'RECORDED'),
                                                                     ('DRAFT',         'CANCELLED'),
                                                                     -- RECORDED
                                                                     ('RECORDED',      'NEEDS_REVIEW'),
                                                                     ('RECORDED',      'CONFIRMED'),
                                                                     ('RECORDED',      'REJECTED'),
                                                                     ('RECORDED',      'CANCELLED'),
                                                                     -- NEEDS_REVIEW
                                                                     ('NEEDS_REVIEW',  'CONFIRMED'),
                                                                     ('NEEDS_REVIEW',  'REJECTED'),
                                                                     ('NEEDS_REVIEW',  'CANCELLED'),
                                                                     -- CONFIRMED
                                                                     ('CONFIRMED',     'CLOSED'),
                                                                     ('CONFIRMED',     'CANCELLED'),
                                                                     -- REJECTED
                                                                     ('REJECTED',      'DRAFT'),
                                                                     ('REJECTED',      'CANCELLED'),
                                                                     ('REJECTED',      'ARCHIVED'),
                                                                     -- CLOSED
                                                                     ('CLOSED',        'ARCHIVED'),
                                                                     -- CANCELLED
                                                                     ('CANCELLED',     'ARCHIVED')
    ON CONFLICT (from_status, to_status) DO NOTHING;

COMMENT ON TABLE incident.status_transitions IS
    'Матрица разрешённых переходов статусов (seed 011). Пустая таблица = запрет всех переходов.';