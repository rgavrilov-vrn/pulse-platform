-- 002_create_schema.sql



CREATE SCHEMA IF NOT EXISTS incident;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'source_type' AND typnamespace = 'incident'::regnamespace) THEN
CREATE TYPE incident.source_type AS ENUM ('TRASSIR', 'LINE', 'MANUAL', 'OTHER');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'creation_mode' AND typnamespace = 'incident'::regnamespace) THEN
CREATE TYPE incident.creation_mode AS ENUM ('INTEGRATION', 'MANUAL', 'IMPORT');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'status_type' AND typnamespace = 'incident'::regnamespace) THEN
CREATE TYPE incident.status_type AS ENUM ('DRAFT', 'RECORDED', 'NEEDS_REVIEW', 'CONFIRMED', 'REJECTED', 'CLOSED', 'CANCELLED', 'ARCHIVED');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'severity_type' AND typnamespace = 'incident'::regnamespace) THEN
CREATE TYPE incident.severity_type AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'material_type' AND typnamespace = 'incident'::regnamespace) THEN
CREATE TYPE incident.material_type AS ENUM ('IMAGE', 'VIDEO', 'DOCUMENT', 'OTHER');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'material_role' AND typnamespace = 'incident'::regnamespace) THEN
CREATE TYPE incident.material_role AS ENUM ('PRIMARY_EVIDENCE', 'ADDITIONAL_EVIDENCE', 'ARCHIVE_FRAGMENT', 'REPORT_ATTACHMENT');
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'comment_type' AND typnamespace = 'incident'::regnamespace) THEN
CREATE TYPE incident.comment_type AS ENUM ('OPERATOR', 'REVIEW', 'RESOLUTION', 'SYSTEM');
END IF;
END;
$$;

CREATE SEQUENCE IF NOT EXISTS incident.incident_number_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE FUNCTION incident.generate_incident_number()
RETURNS VARCHAR LANGUAGE plpgsql AS $$
DECLARE next_number BIGINT;
BEGIN
    next_number := nextval('incident.incident_number_seq');
RETURN 'INC-' || to_char(current_date, 'YYYY') || '-' || lpad(next_number::TEXT, 6, '0');
END;
$$;

CREATE TABLE IF NOT EXISTS incident.incidents (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number VARCHAR(30) NOT NULL UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    source incident.source_type NOT NULL,
    creation_mode incident.creation_mode NOT NULL,
    status incident.status_type NOT NULL DEFAULT 'DRAFT',
    severity incident.severity_type NOT NULL DEFAULT 'MEDIUM',
    event_time TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES auth.users(id),
    updated_by UUID REFERENCES auth.users(id),
    operator_id UUID REFERENCES auth.users(id),
    object_id UUID REFERENCES ref.objects(id),
    zone_id UUID REFERENCES ref.zones(id),
    camera_id UUID REFERENCES ref.cameras(id),
    server_id UUID REFERENCES ref.video_servers(id),
    camera_description TEXT,
    employee_id UUID REFERENCES ref.employees(id),
    department_id UUID REFERENCES ref.departments(id),
    comment TEXT,
    assigned_to UUID REFERENCES auth.users(id),
    due_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    confirmed_by UUID REFERENCES auth.users(id),
    closed_at TIMESTAMPTZ,
    closed_by UUID REFERENCES auth.users(id),
    resolution_comment TEXT,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID REFERENCES auth.users(id),
    cancellation_reason TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT chk_incidents_version CHECK (version > 0),
    CONSTRAINT chk_incidents_dates CHECK (updated_at >= created_at),
    CONSTRAINT chk_incidents_due_date CHECK (due_at IS NULL OR due_at >= created_at)
    );

CREATE OR REPLACE FUNCTION incident.set_incident_number()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.number IS NULL OR NEW.number = '' THEN
        NEW.number := incident.generate_incident_number();
END IF;
RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_set_incident_number ON incident.incidents;
CREATE TRIGGER trg_set_incident_number
    BEFORE INSERT ON incident.incidents
    FOR EACH ROW EXECUTE FUNCTION incident.set_incident_number();

CREATE TABLE IF NOT EXISTS incident.incident_checklist_items (
                                                                 incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    checklist_item_id UUID NOT NULL REFERENCES ref.checklist_items(id) ON DELETE RESTRICT,
    item_name_snapshot VARCHAR(500) NOT NULL,
    category_name_snapshot VARCHAR(255),
    severity incident.severity_type NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES auth.users(id),
    PRIMARY KEY (incident_id, checklist_item_id)
    );

CREATE TABLE IF NOT EXISTS incident.source_events (
                                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source incident.source_type NOT NULL,
    external_event_id VARCHAR(500),
    idempotency_key VARCHAR(500),
    incident_id UUID REFERENCES incident.incidents(id),
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_source_events_status CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'FAILED', 'DUPLICATE')),
    CONSTRAINT chk_source_events_retry_count CHECK (retry_count >= 0)
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_source_events_external_id ON incident.source_events (source, external_event_id) WHERE external_event_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_source_events_idempotency_key ON incident.source_events (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS incident.media_assets (
                                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_provider VARCHAR(50) NOT NULL DEFAULT 'S3',
    storage_bucket VARCHAR(255),
    storage_key VARCHAR(1000) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    file_extension VARCHAR(20),
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    material_type incident.material_type NOT NULL,
    captured_at TIMESTAMPTZ,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploaded_by UUID NOT NULL REFERENCES auth.users(id),
    is_original BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_media_storage_key UNIQUE (storage_provider, storage_key),
    CONSTRAINT chk_media_size CHECK (size_bytes >= 0),
    CONSTRAINT chk_media_sha256 CHECK (sha256 ~ '^[0-9a-fA-F]{64}$')
    );

CREATE TABLE IF NOT EXISTS incident.incident_materials (
                                                           incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    media_asset_id UUID NOT NULL REFERENCES incident.media_assets(id) ON DELETE RESTRICT,
    role incident.material_role NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES auth.users(id),
    PRIMARY KEY (incident_id, media_asset_id)
    );

CREATE TABLE IF NOT EXISTS incident.archive_references (
                                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    reference_type VARCHAR(30) NOT NULL,
    reference_value TEXT NOT NULL,
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    source incident.source_type NOT NULL,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    last_checked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES auth.users(id),
    CONSTRAINT chk_archive_reference_type CHECK (reference_type IN ('URL', 'NATIVE_LINK', 'EVENT_ID', 'FILE')),
    CONSTRAINT chk_archive_reference_period CHECK (end_time IS NULL OR start_time IS NULL OR end_time >= start_time)
    );

CREATE TABLE IF NOT EXISTS incident.comments (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    author_id UUID NOT NULL REFERENCES auth.users(id),
    comment_type incident.comment_type NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES auth.users(id),
    CONSTRAINT chk_comments_body CHECK (length(trim(body)) > 0)
    );

CREATE TABLE IF NOT EXISTS incident.assignments (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    assigned_to UUID NOT NULL REFERENCES auth.users(id),
    assigned_by UUID NOT NULL REFERENCES auth.users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    comment TEXT,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_assignment_due_date CHECK (due_at IS NULL OR due_at >= assigned_at)
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_current_incident_assignment ON incident.assignments (incident_id) WHERE is_current = TRUE;

CREATE TABLE IF NOT EXISTS incident.status_history (
                                                       id BIGSERIAL PRIMARY KEY,
                                                       incident_id UUID NOT NULL REFERENCES incident.incidents(id) ON DELETE RESTRICT,
    from_status incident.status_type,
    to_status incident.status_type NOT NULL,
    changed_by UUID NOT NULL REFERENCES auth.users(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    comment TEXT,
    request_id VARCHAR(100),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
    );

CREATE TABLE IF NOT EXISTS incident.report_jobs (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by UUID NOT NULL REFERENCES auth.users(id),
    report_type VARCHAR(100) NOT NULL,
    output_format VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    filters JSONB NOT NULL DEFAULT '{}'::jsonb,
    options JSONB NOT NULL DEFAULT '{}'::jsonb,
    result_media_id UUID REFERENCES incident.media_assets(id),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    CONSTRAINT chk_report_output_format CHECK (output_format IN ('DOCX', 'PDF', 'XLSX')),
    CONSTRAINT chk_report_status CHECK (status IN ('QUEUED', 'PROCESSING', 'READY', 'FAILED', 'CANCELLED'))
    );

CREATE INDEX IF NOT EXISTS ix_incidents_event_time ON incident.incidents (event_time DESC);
CREATE INDEX IF NOT EXISTS ix_incidents_created_at ON incident.incidents (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_incidents_status ON incident.incidents (status);
CREATE INDEX IF NOT EXISTS ix_incidents_source ON incident.incidents (source);
CREATE INDEX IF NOT EXISTS ix_incidents_operator ON incident.incidents (operator_id);
CREATE INDEX IF NOT EXISTS ix_incidents_assigned_to ON incident.incidents (assigned_to);
CREATE INDEX IF NOT EXISTS ix_incidents_camera ON incident.incidents (camera_id);
CREATE INDEX IF NOT EXISTS ix_incidents_object_zone ON incident.incidents (object_id, zone_id);
CREATE INDEX IF NOT EXISTS ix_incidents_severity_status ON incident.incidents (severity, status);
CREATE INDEX IF NOT EXISTS ix_incidents_register_filter ON incident.incidents (object_id, event_time DESC, status);
CREATE INDEX IF NOT EXISTS ix_incidents_metadata_gin ON incident.incidents USING GIN (metadata);
CREATE INDEX IF NOT EXISTS ix_source_events_payload_gin ON incident.source_events USING GIN (payload);
CREATE INDEX IF NOT EXISTS ix_incident_materials_incident ON incident.incident_materials (incident_id);
CREATE INDEX IF NOT EXISTS ix_status_history_incident ON incident.status_history (incident_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS ix_comments_incident ON incident.comments (incident_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_report_jobs_status ON incident.report_jobs (status, created_at);

CREATE OR REPLACE VIEW incident.incident_list AS
SELECT
    i.id, i.number, i.source, i.creation_mode, i.status, i.severity,
    i.event_time, i.created_at, i.updated_at,
    operator_user.display_name AS operator_name,
    assigned_user.display_name AS assigned_to_name,
    o.name AS object_name, z.name AS zone_name,
    c.name AS camera_name, vs.name AS server_name,
    ci.name AS checklist_item_name, vc.name AS category_name,
    (SELECT COUNT(*) FROM incident.incident_materials im WHERE im.incident_id = i.id) AS material_count
FROM incident.incidents i
         LEFT JOIN auth.users operator_user ON operator_user.id = i.operator_id
         LEFT JOIN auth.users assigned_user ON assigned_user.id = i.assigned_to
         LEFT JOIN ref.objects o ON o.id = i.object_id
         LEFT JOIN ref.zones z ON z.id = i.zone_id
         LEFT JOIN ref.cameras c ON c.id = i.camera_id
         LEFT JOIN ref.video_servers vs ON vs.id = i.server_id
         LEFT JOIN incident.incident_checklist_items ici ON ici.incident_id = i.id
         LEFT JOIN ref.checklist_items ci ON ci.id = ici.checklist_item_id
         LEFT JOIN ref.violation_categories vc ON vc.id = ci.category_id;
