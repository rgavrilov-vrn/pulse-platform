CREATE SCHEMA IF NOT EXISTS ref;


CREATE TABLE IF NOT EXISTS ref.objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS ref.zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    object_id UUID NOT NULL REFERENCES ref.objects(id),
    parent_id UUID REFERENCES ref.zones(id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_zones_object_code UNIQUE (object_id, code)
    );

CREATE TABLE IF NOT EXISTS ref.departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS ref.employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    personnel_no VARCHAR(100) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    department_id UUID REFERENCES ref.departments(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS ref.video_servers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source incident.source_type NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    host VARCHAR(255),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_video_servers_source_external UNIQUE (source, external_id)
    );

CREATE TABLE IF NOT EXISTS ref.cameras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id UUID REFERENCES ref.video_servers(id),
    object_id UUID REFERENCES ref.objects(id),
    zone_id UUID REFERENCES ref.zones(id),
    source incident.source_type NOT NULL,
    external_id VARCHAR(255),
    code VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    channel VARCHAR(100),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cameras_source_external UNIQUE (source, external_id)
    );

CREATE TABLE IF NOT EXISTS ref.violation_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS ref.checklist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES ref.violation_categories(id),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    requires_image BOOLEAN NOT NULL DEFAULT FALSE,
    requires_comment BOOLEAN NOT NULL DEFAULT FALSE,
    requires_video BOOLEAN NOT NULL DEFAULT FALSE,
    default_severity incident.severity_type NOT NULL DEFAULT 'MEDIUM',
    comment_template TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS ref.object_checklist_items (
    object_id UUID NOT NULL REFERENCES ref.objects(id),
    checklist_item_id UUID NOT NULL REFERENCES ref.checklist_items(id),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (object_id, checklist_item_id)
    );

CREATE TABLE IF NOT EXISTS ref.status_transitions (
    from_status incident.status_type NOT NULL,
    to_status incident.status_type NOT NULL,
    PRIMARY KEY (from_status, to_status)
    );
