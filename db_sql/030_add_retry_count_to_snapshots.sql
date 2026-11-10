ALTER TABLE trassir.snapshots
    ADD COLUMN IF NOT EXISTS retry_count integer NOT NULL DEFAULT 0;
