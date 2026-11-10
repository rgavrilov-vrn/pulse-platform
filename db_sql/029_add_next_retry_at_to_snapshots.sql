ALTER TABLE trassir.snapshots
    ADD COLUMN IF NOT EXISTS next_retry_at timestamptz;
