-- Delivery addresses: run once in the Supabase SQL editor before starting the backend with this change.
-- customer_profiles has no JPA entity, so Hibernate (ddl-auto=update) won't add its column; the orders
-- columns would be added on startup anyway and are listed here so the schema is complete either way.
ALTER TABLE customer_profiles ADD COLUMN IF NOT EXISTS address VARCHAR(255);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_latitude DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_longitude DOUBLE PRECISION;
