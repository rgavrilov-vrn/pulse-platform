-- 999_check_installation.sql
-- Только новая установка. Запустить файл целиком в pulse_platform.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'pulse_platform' THEN
        RAISE EXCEPTION 'Подключитесь к pulse_platform';
    END IF;
END $$;
SET LOCAL ROLE pulse_platform_owner;
SET LOCAL search_path = public, pg_catalog;

DO $check$
DECLARE
    s text;
    t text;
    obj record;
BEGIN
    FOREACH s IN ARRAY ARRAY['auth','audit','outbox','ref','incident','videoserver','goodwan'] LOOP
        (SELECT 1 FROM pg_namespace WHERE nspname=s AND nspowner='pulse_platform_owner'::regrole) THEN
            RAISE EXCEPTION 'Схема % отсутствует или имеет неверного владельца', s;
        END IF;
        FOR obj IN SELECT c.oid, c.relname, c.relowner, c.relkind FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=s AND c.relkind IN ('r','v','S') LOOP
            IF obj.relowner <> 'pulse_platform_owner'::regrole THEN RAISE EXCEPTION 'Неверный владелец %.%', s, obj.relname; END IF;
            IF obj.relkind IN ('r','v') THEN
                IF NOT has_table_privilege('pulse_platform_app',obj.oid,'SELECT') THEN RAISE EXCEPTION 'Нет чтения app: %.%',s,obj.relname; END IF;
                IF s <> 'auth' AND NOT has_table_privilege('pulse_platform_readonly',obj.oid,'SELECT') THEN RAISE EXCEPTION 'Нет чтения readonly: %.%',s,obj.relname; END IF;
                IF has_table_privilege('pulse_platform_readonly',obj.oid,'INSERT,UPDATE,DELETE,TRUNCATE') THEN RAISE EXCEPTION 'readonly может изменять %.%',s,obj.relname; END IF;
            END IF;
        END LOOP;
    END LOOP;
    FOREACH t IN ARRAY ARRAY['auth.users','auth.roles','auth.permissions','auth.role_permissions','audit.audit_log','outbox.outbox_events','outbox.processed_events','ref.objects','ref.zones','ref.departments','ref.employees','incident.incidents','incident.media_assets','incident.incident_materials','incident.incident_checklist_items','incident.status_transitions','incident.incident_list','videoserver.video_servers','videoserver.cameras','videoserver.channels','videoserver.raw_video_events','videoserver.normalized_video_events','goodwan.raw_data_history'] LOOP
        IF to_regclass(t) IS NULL THEN RAISE EXCEPTION 'Отсутствует %',t; END IF;
    END LOOP;
    IF has_table_privilege('pulse_platform_app','audit.audit_log','UPDATE,DELETE,TRUNCATE') THEN RAISE EXCEPTION 'Аудит не защищён от изменения'; END IF;
    IF NOT has_table_privilege('pulse_platform_app','audit.audit_log','INSERT') THEN RAISE EXCEPTION 'Нет INSERT в аудит'; END IF;
    IF has_schema_privilege('pulse_platform_readonly','auth','USAGE') THEN RAISE EXCEPTION 'readonly имеет доступ к auth'; END IF;
    IF (SELECT count(*) FROM incident.category_groups) <> 2 OR (SELECT count(*) FROM incident.violation_categories) <> 2 OR (SELECT count(*) FROM incident.checklist_items) <> 21 THEN RAISE EXCEPTION 'Неполный каталог инцидентов'; END IF;
    IF (SELECT count(*) FROM incident.status_transitions) <> 15 THEN RAISE EXCEPTION 'Неполная матрица переходов'; END IF;
    IF (SELECT count(*) FROM ref.objects) <> 7 OR (SELECT count(*) FROM ref.zones) <> 49 OR (SELECT count(*) FROM ref.departments) <> 3 THEN RAISE EXCEPTION 'Неполные справочники ref'; END IF;
    (SELECT 1 FROM auth.users u JOIN auth.user_roles ur ON ur.user_id=u.id JOIN auth.roles r ON r.id=ur.role_id WHERE u.id='00000000-0000-0000-0000-000000000001' AND u.login='__system__' AND u.password_hash='!' AND r.code='INTEGRATION') THEN RAISE EXCEPTION 'Нет системного пользователя'; END IF;
    (SELECT 1 FROM auth.users u JOIN auth.user_roles ur ON ur.user_id=u.id JOIN auth.roles r ON r.id=ur.role_id WHERE u.is_active AND u.password_hash <> '!' AND r.code='SUPERUSER') THEN RAISE EXCEPTION 'Не создан администратор'; END IF;
    IF EXISTS (SELECT 1 FROM auth.permissions p WHERE NOT EXISTS (SELECT 1 FROM auth.role_permissions rp JOIN auth.roles r ON r.id=rp.role_id WHERE r.code='SUPERUSER' AND rp.permission_id=p.id)) THEN RAISE EXCEPTION 'Неполные права SUPERUSER'; END IF;
END $check$;
SELECT 'OK: структура, начальные данные и основные права проверены' AS installation_check;

COMMIT;
