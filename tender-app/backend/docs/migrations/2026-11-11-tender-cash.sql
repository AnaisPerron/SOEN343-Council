CREATE TABLE IF NOT EXISTS tender_cash_transactions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT NOT NULL REFERENCES orders(id),
    reason VARCHAR(24) NOT NULL,
    tenders BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT tender_cash_order_reason_unique UNIQUE (order_id, reason)
);
CREATE INDEX IF NOT EXISTS tender_cash_customer_idx ON tender_cash_transactions (customer_id);
