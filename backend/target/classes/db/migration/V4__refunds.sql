-- V4: Refunds table (Phase 8).

CREATE TABLE refunds (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    refund_id           VARCHAR(32)     NOT NULL UNIQUE,
    payment_id          VARCHAR(32)     NOT NULL REFERENCES payments(id),
    amount              NUMERIC(19,2)   NOT NULL CHECK (amount > 0),
    reason              VARCHAR(255),
    status              VARCHAR(32)     NOT NULL,
    provider_reference  VARCHAR(64),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_refund_status CHECK (status IN (
        'REFUND_REQUESTED','REFUND_PROCESSING','REFUNDED','REFUND_FAILED'
    ))
);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
