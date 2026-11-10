BEGIN;

ALTER TABLE trassir.events
    ADD COLUMN IF NOT EXISTS status varchar NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS last_error text,
    ADD COLUMN IF NOT EXISTS retry_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_retry_at timestamptz;

UPDATE trassir.events
SET status = CASE
    WHEN processing_error IS NULL THEN 'PROCESSED'
    ELSE 'FAILED'
END
WHERE status = 'PENDING';

UPDATE trassir.events
SET last_error = processing_error
WHERE status = 'FAILED'
  AND last_error IS NULL;

UPDATE trassir.events
SET next_retry_at = now()
WHERE status = 'FAILED'
  AND next_retry_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_trassir_events_status_next_retry
    ON trassir.events (status, next_retry_at);

COMMIT;
