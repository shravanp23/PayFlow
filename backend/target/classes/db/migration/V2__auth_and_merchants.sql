-- V2: Auth + merchant + API key tables (Phase 4).

CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(32)     NOT NULL CHECK (role IN ('ADMIN','DEVELOPER','VIEWER')),
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE merchants (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
    webhook_url     VARCHAR(512),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE api_keys (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID            NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    key_hash        VARCHAR(255)    NOT NULL UNIQUE,
    key_prefix      VARCHAR(32)     NOT NULL,
    environment     VARCHAR(16)     NOT NULL CHECK (environment IN ('TEST','LIVE')),
    revoked         BOOLEAN         NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_merchant_id ON api_keys (merchant_id);

-- payments.merchant_id now references a real merchant.
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id);
