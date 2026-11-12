-- V18__auth_revoked_jti.sql
-- FIX #48: отзыв JWT до истечения. Раньше единственный способ «убить» токен —
-- дождаться exp (60 минут по умолчанию). При компрометации — критично.
--
-- Храним только jti + expires_at (после exp запись не нужна — токен уже мёртв).
-- Scheduled cleanup удаляет истёкшие.

CREATE TABLE auth.revoked_jti (
                                  jti         VARCHAR(64)  PRIMARY KEY,
                                  expires_at  TIMESTAMPTZ  NOT NULL,
                                  revoked_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                  reason      VARCHAR(100)
);

CREATE INDEX ix_revoked_jti_expires_at ON auth.revoked_jti (expires_at);

COMMENT ON TABLE auth.revoked_jti IS
    'Отозванные JWT по jti. Cleanup — по expires_at (после exp токен невалиден сам по себе).';