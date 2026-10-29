-- V3: Durable idempotency record (Phase 5). Redis is the fast path; this table
-- is the source of truth if Redis is unavailable or a key's TTL has expired.

CREATE TABLE idempotency_keys (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id         UUID            NOT NULL REFERENCES merchants(id),
    idempotency_key     VARCHAR(255)    NOT NULL,
    payment_id          VARCHAR(32)     NOT NULL REFERENCES payments(id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_idempotency_merchant_key UNIQUE (merchant_id, idempotency_key)
);

CREATE INDEX idx_idempotency_keys_payment_id ON idempotency_keys (payment_id);
