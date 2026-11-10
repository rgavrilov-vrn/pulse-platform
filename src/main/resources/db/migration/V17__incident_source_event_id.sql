-- V17__incident_source_event_id.sql
-- FIX #58: дедупликация integration-инцидентов на уровне БД.
-- Раньше повторный ingest того же video-события (агент переслал пакет)
-- создавал второй инцидент — полагались только на processed_events
-- в outbox, который живёт в другой транзакции.

ALTER TABLE incident.incidents
    ADD COLUMN IF NOT EXISTS source_event_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS ux_incidents_source_event_id
    ON incident.incidents (source, source_event_id)
    WHERE source_event_id IS NOT NULL;

COMMENT ON COLUMN incident.incidents.source_event_id IS
    'ID события во внешней системе. Для video-инцидентов = VideoEventNormalized.eventId. '
    'Unique (source, source_event_id) гарантирует идемпотентность ingest.';