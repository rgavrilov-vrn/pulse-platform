ALTER TABLE trassir.snapshots
    ADD COLUMN IF NOT EXISTS status varchar(32) NOT NULL DEFAULT 'PENDING';
