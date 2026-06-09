-- ============================================================================
-- V1 — Esquema de plataforma: planes, establecimientos (tenants), usuarios,
-- tokens JWT, reset de contraseña, uso de plan y tokens del proveedor Factus.
-- Convención común: id UUID PK, version BIGINT, auditoría created/updated.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── Planes ──────────────────────────────────────────────────────────────────
CREATE TABLE plans (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version               BIGINT       NOT NULL DEFAULT 0,
    code                  VARCHAR(40)  NOT NULL UNIQUE,
    name                  VARCHAR(120) NOT NULL,
    monthly_invoice_limit INTEGER,                       -- NULL = ilimitado
    price                 NUMERIC(15,2) NOT NULL DEFAULT 0,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    created_by            UUID,
    updated_by            UUID
);

-- ── Establecimientos (tenant) ───────────────────────────────────────────────
CREATE TABLE establishments (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version            BIGINT       NOT NULL DEFAULT 0,
    name               VARCHAR(180) NOT NULL,
    identification     VARCHAR(40)  NOT NULL,            -- NIT/CC sin DV
    dv                 VARCHAR(2),
    address            VARCHAR(255),
    phone              VARCHAR(40),
    email              VARCHAR(180),
    municipality_code  VARCHAR(10),
    numbering_range_id INTEGER,                          -- rango DIAN en Factus
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    created_by         UUID,
    updated_by         UUID
);

-- ── Usuarios ────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version               BIGINT       NOT NULL DEFAULT 0,
    email                 VARCHAR(180) NOT NULL UNIQUE,
    password              VARCHAR(72)  NOT NULL,         -- BCrypt
    full_name             VARCHAR(180),
    role                  VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- ADMIN | USER
    plan_id               UUID         REFERENCES plans(id),
    establishment_id      UUID         REFERENCES establishments(id),
    enabled               BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    created_by            UUID,
    updated_by            UUID
);

-- ── Tokens JWT persistidos (revocables) ─────────────────────────────────────
CREATE TABLE tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    version    BIGINT      NOT NULL DEFAULT 0,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      TEXT        NOT NULL UNIQUE,
    type       VARCHAR(20) NOT NULL,                     -- ACCESS | REFRESH
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_tokens_user ON tokens(user_id);

-- ── Tokens de reset de contraseña (almacenados HASHEADOS) ───────────────────
CREATE TABLE password_reset_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    version    BIGINT       NOT NULL DEFAULT 0,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_pwd_reset_user ON password_reset_tokens(user_id);

-- ── Uso de plan por periodo (contador atómico mensual) ──────────────────────
CREATE TABLE plan_usage (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    version          BIGINT      NOT NULL DEFAULT 0,
    establishment_id UUID        NOT NULL REFERENCES establishments(id) ON DELETE CASCADE,
    period           VARCHAR(7)  NOT NULL,               -- YYYY-MM
    invoice_count    INTEGER     NOT NULL DEFAULT 0,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    CONSTRAINT uq_plan_usage UNIQUE (establishment_id, period)
);

-- ── Tokens del proveedor externo Factus (CIFRADOS AES/GCM) ──────────────────
CREATE TABLE factus_tokens (
    id                      UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    version                 BIGINT    NOT NULL DEFAULT 0,
    establishment_id        UUID      REFERENCES establishments(id) ON DELETE CASCADE,
    access_token_encrypted  TEXT      NOT NULL,
    refresh_token_encrypted TEXT,
    expires_at              TIMESTAMP NOT NULL,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP NOT NULL,
    created_by              UUID,
    updated_by              UUID
);
CREATE INDEX idx_factus_tokens_est ON factus_tokens(establishment_id);
