-- V16__trassir_stream_id_backfill.sql
-- FIX #30: snapshot_streams.stream_id раньше = snapshot UUID, из-за чего
-- таблица росла 1:1 со снапшотами. Теперь stream_id = server_id
-- (стабильный идентификатор агента/сервера).
--
-- Старые строки несогласованы — проще пересоздать: при следующем
-- snapshot по серверу запись появится автоматически.

TRUNCATE trassir.snapshot_streams;

COMMENT ON COLUMN trassir.snapshot_streams.stream_id IS
    'Стабильный идентификатор потока (обычно server_id). Не snapshot UUID.';