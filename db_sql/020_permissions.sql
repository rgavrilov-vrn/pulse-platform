-- 020_permissions.sql
-- Гранты на бизнес-схемы для pulse_platform_app и pulse_platform_readonly.
-- Должен выполняться владельцем БД / суперпользователем — тем же, кто
-- создавал объекты в предыдущих миграциях (иначе default privileges
-- не применятся к объектам, созданным другой ролью).

-- ============================================================================
-- pulse_platform_app (read/write на бизнес-схемах)
-- ============================================================================
GRANT USAGE ON SCHEMA auth, ref, incident, videoserver, outbox, audit, goodwan
    TO pulse_platform_app;

GRANT SELECT, INSERT, UPDATE, DELETE
      ON ALL TABLES IN SCHEMA auth, ref, incident, videoserver, outbox, goodwan
          TO pulse_platform_app;

-- audit — append-only: приложению запрещены UPDATE/DELETE.
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA audit TO pulse_platform_app;

GRANT USAGE, SELECT
             ON ALL SEQUENCES IN SCHEMA auth, ref, incident, videoserver, outbox, audit, goodwan
                 TO pulse_platform_app;

-- ============================================================================
-- pulse_platform_readonly (только SELECT на бизнес-данных)
-- ============================================================================
GRANT USAGE ON SCHEMA ref, incident, videoserver, outbox, audit, goodwan
    TO pulse_platform_readonly;

GRANT SELECT
      ON ALL TABLES IN SCHEMA ref, incident, videoserver, outbox, audit, goodwan
          TO pulse_platform_readonly;

-- ============================================================================
-- Default privileges для будущих объектов
-- ============================================================================
ALTER DEFAULT PRIVILEGES IN SCHEMA auth
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA ref
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA incident
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA videoserver
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA outbox
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA goodwan
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pulse_platform_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT SELECT, INSERT ON TABLES TO pulse_platform_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA auth
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA ref
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA incident
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA videoserver
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA outbox
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA goodwan
    GRANT USAGE, SELECT ON SEQUENCES TO pulse_platform_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA ref
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA incident
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA videoserver
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA outbox
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit
    GRANT SELECT ON TABLES TO pulse_platform_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA goodwan
    GRANT SELECT ON TABLES TO pulse_platform_readonly;

-- ============================================================================
-- Рекомендуемые дефолты для ролей приложения (выполнить один раз
-- суперпользователем отдельно от миграций):
--
--   ALTER ROLE pulse_platform_app       SET search_path = pg_catalog, public;
--   ALTER ROLE pulse_platform_readonly  SET search_path = pg_catalog, public;
--
-- Без этого приложение может попасть в ту же ловушку 3F000/42883,
-- что и миграции, если его инструмент/драйвер выставляет search_path = ''.
-- ============================================================================