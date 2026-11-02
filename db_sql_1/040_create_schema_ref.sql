-- 040_create_schema_ref.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

CREATE SCHEMA ref;

CREATE TABLE ref.objects (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE ref.zones (
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

CREATE TABLE ref.departments (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE ref.employees (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    personnel_no VARCHAR(100) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    department_id UUID REFERENCES ref.departments(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
COMMENT ON SCHEMA ref IS 'Справочные данные: объекты, зоны, подразделения, сотрудники. Автономна.';

COMMENT ON TABLE  ref.objects IS 'Объекты (площадки, здания, цеха). Корень пространственной иерархии.';
COMMENT ON COLUMN ref.objects.code      IS 'Внутренний код объекта';
COMMENT ON COLUMN ref.objects.address   IS 'Физический адрес';
COMMENT ON COLUMN ref.objects.is_active IS 'Объект активен';

COMMENT ON TABLE  ref.zones IS 'Зоны внутри объектов (иерархические через parent_id).';
COMMENT ON COLUMN ref.zones.object_id IS 'Родительский объект';
COMMENT ON COLUMN ref.zones.parent_id IS 'Родительская зона (null — корневая зона объекта)';
COMMENT ON COLUMN ref.zones.code      IS 'Код зоны (уникален в пределах объекта)';

COMMENT ON TABLE  ref.departments IS 'Организационные подразделения.';
COMMENT ON TABLE  ref.employees   IS 'Сотрудники.';
COMMENT ON COLUMN ref.employees.personnel_no  IS 'Табельный номер (уникален)';
COMMENT ON COLUMN ref.employees.department_id IS 'Подразделение';

COMMIT;
