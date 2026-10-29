-- V5: Webhook event log + delivery attempt tracking (Phase 9).

CREATE TABLE webhook_events (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            VARCHAR(64)     NOT NULL UNIQUE,
    event_type          VARCHAR(64)     NOT NULL,
    payment_id          VARCHAR(32)     NOT NULL REFERENCES payments(id),
    merchant_id         UUID            NOT NULL REFERENCES merchants(id),
    payload             TEXT            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE webhook_deliveries (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_event_id    UUID            NOT NULL REFERENCES webhook_events(id),
    target_url          VARCHAR(512)    NOT NULL,
    status              VARCHAR(16)     NOT NULL CHECK (status IN ('PENDING','DELIVERED','FAILED','RETRYING')),
    attempt_count       INT             NOT NULL DEFAULT 0,
    last_response_status INT,
    last_attempt_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_deliveries_event_id ON webhook_deliveries (webhook_event_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries (status);
