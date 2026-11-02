-- От имени pulse_platform_app (или через SET ROLE) — все должны отработать:
SET ROLE pulse_platform_app;

SELECT count(*) FROM incident.incidents;
SELECT count(*) FROM incident.incident_list;
SELECT count(*) FROM incident.context_refs;
SELECT count(*) FROM videoserver.cameras;
SELECT count(*) FROM videoserver.raw_video_events;
SELECT count(*) FROM ref.objects;
SELECT count(*) FROM auth.users;
SELECT count(*) FROM outbox.outbox_events;
SELECT count(*) FROM audit.audit_log;              -- readonly
SELECT nextval('incident.incident_number_seq');    -- USAGE, SELECT

-- А это должно упасть (append-only audit):
UPDATE audit.audit_log SET action = 'hack' WHERE id = -1;

RESET ROLE;

-- От имени readonly:
SET ROLE pulse_platform_readonly;
SELECT count(*) FROM incident.incidents;           -- OK

-- Должно упасть (permission denied):
INSERT INTO incident.incidents (number, source, creation_mode, category_id, event_time, created_by)
VALUES ('INC-TEST-000001', 'manual', 'MANUAL', gen_random_uuid(), now(), gen_random_uuid());

RESET ROLE;