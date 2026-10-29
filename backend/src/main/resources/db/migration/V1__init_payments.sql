-- V1: Core payment schema (Phase 2-3 of the build roadmap).
-- Later phases add: users, merchants, api_keys, refunds, idempotency_keys,
-- webhook_events, webhook_deliveries, audit_logs, reconciliation_*, provider_transactions.
-- Each gets its OWN migration file (V2, V3, ...) — never edit this file after it ships.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE payments (
    id                  VARCHAR(32)     PRIMARY KEY,
    amount              NUMERIC(19,2)   NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)      NOT NULL,
    merchant_id         UUID            NOT NULL,
    payment_method      VARCHAR(32)     NOT NULL,
    provider            VARCHAR(64),
    status              VARCHAR(32)     NOT NULL,
    customer_reference  VARCHAR(255),
    description         TEXT,
    idempotency_key     VARCHAR(255),
    retry_count         INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT chk_payment_status CHECK (status IN (
        'CREATED','PROCESSING','SUCCESS','FAILED','TIMEOUT','RETRYING',
        'CANCELLED','REFUND_REQUESTED','REFUND_PROCESSING','REFUNDED','REFUND_FAILED'
    ))
);

-- The idempotency safety net: two rows can never share the same
-- (merchant_id, idempotency_key) pair, even under concurrent inserts.
CREATE UNIQUE INDEX uq_payments_merchant_idempotency
    ON payments (merchant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_merchant_id ON payments (merchant_id);
CREATE INDEX idx_payments_created_at ON payments (created_at DESC);
