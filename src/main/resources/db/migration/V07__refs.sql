-- V07__refs.sql
-- Справочники: объекты, зоны, подразделения, сотрудники.

SET search_path = pg_catalog, public;

CREATE TABLE ref.objects (
                             id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             code        VARCHAR(100) NOT NULL UNIQUE,
                             name        VARCHAR(255) NOT NULL,
                             description TEXT,
                             address     TEXT,
                             is_active   BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ref.zones (
                           id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           object_id   UUID NOT NULL REFERENCES ref.objects(id),
                           parent_id   UUID REFERENCES ref.zones(id),
                           code        VARCHAR(100) NOT NULL,
                           name        VARCHAR(255) NOT NULL,
                           description TEXT,
                           is_active   BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                           CONSTRAINT uq_zones_object_code UNIQUE (object_id, code)
);

-- Гарантия, что parent_id — из того же объекта (находка #106)
ALTER TABLE ref.zones
    ADD CONSTRAINT fk_zones_parent_same_object
        FOREIGN KEY (object_id, parent_id) REFERENCES ref.zones(object_id, id)
            DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ref.zones ADD CONSTRAINT uq_zones_object_id UNIQUE (object_id, id);

CREATE INDEX ix_zones_parent ON ref.zones (parent_id);

CREATE TABLE ref.departments (
                                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 code        VARCHAR(100) NOT NULL UNIQUE,
                                 name        VARCHAR(255) NOT NULL,
                                 is_active   BOOLEAN NOT NULL DEFAULT TRUE,
                                 created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ref.employees (
                               id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               personnel_no  VARCHAR(100) UNIQUE,
                               full_name     VARCHAR(255) NOT NULL,
                               department_id UUID REFERENCES ref.departments(id),
                               is_active     BOOLEAN NOT NULL DEFAULT TRUE,
                               created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_employees_department ON ref.employees (department_id);

CREATE TRIGGER trg_objects_updated_at     BEFORE UPDATE ON ref.objects     FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();
CREATE TRIGGER trg_zones_updated_at       BEFORE UPDATE ON ref.zones       FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();
CREATE TRIGGER trg_departments_updated_at BEFORE UPDATE ON ref.departments FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();
CREATE TRIGGER trg_employees_updated_at   BEFORE UPDATE ON ref.employees   FOR EACH ROW EXECUTE FUNCTION public.tg_set_updated_at();