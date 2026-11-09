-- Application database permissions for the trassir schema.

BEGIN;

GRANT USAGE ON SCHEMA trassir
    TO pulse_platform_app, pulse_platform_readonly;

GRANT SELECT, INSERT
    ON trassir.ingest_raw
    TO pulse_platform_app;

GRANT SELECT, INSERT
    ON trassir.events
    TO pulse_platform_app;

GRANT UPDATE (processing_error)
    ON trassir.events
    TO pulse_platform_app;

GRANT SELECT, INSERT
    ON trassir.system_events
    TO pulse_platform_app;

GRANT SELECT, INSERT, UPDATE
    ON trassir.snapshot_streams,
       trassir.snapshots,
       trassir.snapshot_parts,
       trassir.snapshot_objects,
       trassir.objects,
       trassir.agent_heartbeats
    TO pulse_platform_app;

GRANT SELECT, INSERT, UPDATE
    ON trassir.server_state
    TO pulse_platform_app;

GRANT SELECT
    ON ALL TABLES IN SCHEMA trassir
    TO pulse_platform_readonly;

COMMIT;
