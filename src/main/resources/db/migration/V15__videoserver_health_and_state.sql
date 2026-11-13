-- V15__videoserver_health_and_state.sql
-- (3b) server_health + (3c) state-tracking на cameras/channels
SET search_path = pg_catalog, public;

-- ============================================================
-- 3b: Server health (values из объекта class_name='Server')
-- ============================================================
CREATE TABLE videoserver.server_health (
                                           video_server_id              UUID PRIMARY KEY
                                               REFERENCES videoserver.video_servers(id) ON DELETE CASCADE,
                                           observed_at                  TIMESTAMPTZ NOT NULL,

    -- Идентификация
                                           server_name                  TEXT,                       -- 'video81'
                                           startup_at                   TIMESTAMPTZ,

    -- Health
                                           server_ok                    BOOLEAN,
                                           cpu_usage                    REAL,
                                           gpu_usage                    REAL,
                                           db_connected                 BOOLEAN,
                                           db_is_slow                   BOOLEAN,
                                           db_last_error                TEXT,
                                           scripts_ok                   INT,
                                           scripts_total                INT,

    -- Disks / archive
                                           disks_error_count            INT,
                                           disks_is_slow                BOOLEAN,
                                           disks_last_error             TEXT,
                                           disks_stat_main_days         REAL,
                                           disks_stat_subs_days         REAL,

    -- Devices
                                           ip_devices_total             INT,
                                           ip_devices_connected         INT,
                                           channels_network_total       INT,
                                           channels_network_online      INT,

    -- Network
                                           network_should_be_connected  BOOLEAN,
                                           network_really_connected     BOOLEAN,
                                           network_last_error           TEXT,

    -- Raw values (для forward compatibility)
                                           raw_values                   JSONB NOT NULL DEFAULT '{}'::jsonb,

                                           updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE videoserver.server_health IS
    'Health-метрики TRASSIR-сервера, извлекаемые из agent.objects_snapshot (объект class_name=Server).';

-- ============================================================
-- 3c: state-tracking на cameras/channels для derived events
-- ============================================================
ALTER TABLE videoserver.cameras
    ADD COLUMN last_connection_status TEXT,
    ADD COLUMN last_connection_at     TIMESTAMPTZ,
    ADD COLUMN last_ip_address        TEXT,
    ADD COLUMN last_model             TEXT,
    ADD COLUMN last_family            TEXT;

COMMENT ON COLUMN videoserver.cameras.last_connection_status IS
    'Последнее values.connection IP Device (Connected / Needs Attention / Disconnected).';

ALTER TABLE videoserver.channels
    ADD COLUMN last_signal          BOOLEAN,
    ADD COLUMN last_recording_local BOOLEAN,
    ADD COLUMN last_grabber_error   TEXT,
    ADD COLUMN last_fps_main        REAL,
    ADD COLUMN last_kbps_main       REAL,
    ADD COLUMN last_signal_at       TIMESTAMPTZ;

COMMENT ON COLUMN videoserver.channels.last_signal IS
    'Последнее values.signal Channel (TRUE = есть видеопоток).';
COMMENT ON COLUMN videoserver.channels.last_recording_local IS
    'Последнее values.recording_local (TRUE = идёт запись локально).';