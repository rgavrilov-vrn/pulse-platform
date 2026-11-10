ALTER TABLE trassir.snapshots
    ADD COLUMN IF NOT EXISTS external_snapshot_id uuid;

UPDATE trassir.snapshots
SET external_snapshot_id = id
WHERE external_snapshot_id IS NULL;

ALTER TABLE trassir.snapshots
    ALTER COLUMN external_snapshot_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_trassir_snapshots_external_snapshot_id
    ON trassir.snapshots (external_snapshot_id);
