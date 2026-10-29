-- 004_create_schema_auth.sql

CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.users
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255),
    login CITEXT NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email CITEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS auth.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS auth.user_roles (
    user_id UUID NOT NULL REFERENCES auth.users(id),
    role_id UUID NOT NULL REFERENCES auth.roles(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES auth.users(id),
    PRIMARY KEY (user_id, role_id)
    );

INSERT INTO auth.users (
    login,
    password_hash,
    display_name,
    email,
    is_active
)
VALUES (
           'admin',
           '$2b$12$90RI9oyUR3E4cf8gmNukBe4RJjVOrOH6SzOIlN0ACDetEzbZA2hNW',
           'Administrator',
           'admin@example.com',
           TRUE
       )
    ON CONFLICT (login) DO NOTHING;


-- 1. Создать роль ADMIN, если её ещё нет
INSERT INTO auth.roles (
    code,
    name,
    description
)
VALUES (
           'ADMIN',
           'Administrator',
           'Full administrative access'
       )
    ON CONFLICT (code) DO NOTHING;

-- 2. Назначить роль ADMIN пользователю admin
INSERT INTO auth.user_roles (
    user_id,
    role_id
)
SELECT u.id,
       r.id
FROM auth.users u
         JOIN auth.roles r
              ON r.code = 'ADMIN'
WHERE u.login = 'admin'
    ON CONFLICT (user_id, role_id) DO NOTHING;