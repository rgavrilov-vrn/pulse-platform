-- V01__schemas.sql
SET search_path = pg_catalog, public;

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS outbox;
CREATE SCHEMA IF NOT EXISTS ref;
CREATE SCHEMA IF NOT EXISTS incident;
CREATE SCHEMA IF NOT EXISTS videoserver;
CREATE SCHEMA IF NOT EXISTS goodwan;
CREATE SCHEMA IF NOT EXISTS trassir;

COMMENT ON SCHEMA auth        IS 'Аутентификация и авторизация: пользователи, роли, пермишены.';
COMMENT ON SCHEMA audit       IS 'Аудит действий: append-only журнал.';
COMMENT ON SCHEMA outbox      IS 'Transactional outbox: надёжная публикация доменных событий.';
COMMENT ON SCHEMA ref         IS 'Справочные данные: объекты, зоны, подразделения, сотрудники.';
COMMENT ON SCHEMA incident    IS 'Инциденты: доменно-нейтральная сущность и справочники домена.';
COMMENT ON SCHEMA videoserver IS 'Домен видео: серверы, камеры, каналы, события.';
COMMENT ON SCHEMA goodwan     IS 'Сырые данные внешнего устройства goodwan (append-only).';
COMMENT ON SCHEMA trassir     IS 'Телеметрия TRASSIR: снапшоты, события, состояние серверов.';