-- V6: Reconciliation engine tables (Phase 10).

CREATE TABLE reconciliation_runs (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id              VARCHAR(32)     NOT NULL UNIQUE,
    status              VARCHAR(16)     NOT NULL CHECK (status IN ('RUNNING','COMPLETED')),
    matched_count       INT             NOT NULL DEFAULT 0,
    mismatched_count    INT             NOT NULL DEFAULT 0,
    missing_count       INT             NOT NULL DEFAULT 0,
    duplicate_count     INT             NOT NULL DEFAULT 0,
    started_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ
);

CREATE TABLE provider_transactions (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_run_id   UUID            NOT NULL REFERENCES reconciliation_runs(id),
    payment_id              VARCHAR(32)     REFERENCES payments(id),
    provider_reference      VARCHAR(64)     NOT NULL,
    amount                  NUMERIC(19,2)   NOT NULL,
    status                  VARCHAR(32)     NOT NULL,
    settled_at              TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE reconciliation_items (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_run_id   UUID            NOT NULL REFERENCES reconciliation_runs(id),
    payment_id              VARCHAR(32),
    provider_reference      VARCHAR(64),
    status                  VARCHAR(32)     NOT NULL CHECK (status IN (
        'MATCHED','MISSING_INTERNAL','MISSING_PROVIDER','AMOUNT_MISMATCH','STATUS_MISMATCH','DUPLICATE'
    )),
    internal_amount         NUMERIC(19,2),
    provider_amount         NUMERIC(19,2),
    internal_status         VARCHAR(32),
    provider_status         VARCHAR(32)
);

CREATE INDEX idx_provider_transactions_run_id ON provider_transactions (reconciliation_run_id);
CREATE INDEX idx_reconciliation_items_run_id ON reconciliation_items (reconciliation_run_id);
CREATE INDEX idx_reconciliation_items_status ON reconciliation_items (status);
