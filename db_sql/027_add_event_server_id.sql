BEGIN;

ALTER TABLE trassir.events
    ADD COLUMN IF NOT EXISTS server_id text;

UPDATE trassir.events e
SET server_id = r.server_id
FROM trassir.ingest_raw r
WHERE e.raw_id = r.id
  AND e.server_id IS NULL;

ALTER TABLE trassir.events
    ALTER COLUMN server_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_trassir_events_server_id
    ON trassir.events (server_id);

COMMIT;
