INSERT INTO health_check (id, status) VALUES (1, 'OK');

-- customer_profiles has no JPA entity, so Hibernate doesn't create it; same columns as the real schema
CREATE TABLE IF NOT EXISTS customer_profiles (
    user_id BIGINT PRIMARY KEY,
    td_cash_balance NUMERIC(12,2) DEFAULT 0 NOT NULL,
    address VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);
