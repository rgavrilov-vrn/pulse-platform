-- Подключение postgres. Autocommit. НЕ оборачивать в транзакцию.
CREATE DATABASE pulse_platform OWNER pulse_platform_owner ENCODING 'UTF8' TEMPLATE template0;
COMMENT ON DATABASE pulse_platform IS 'Pulse Platform: auth, audit, outbox, ref, incident, videoserver, goodwan';
