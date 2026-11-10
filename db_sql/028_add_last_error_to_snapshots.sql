ALTER TABLE trassir.snapshots
    ADD COLUMN IF NOT EXISTS last_error text;
