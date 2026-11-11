CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(255), phone VARCHAR(20), role VARCHAR(20), password_hash TEXT);
CREATE TABLE restaurants (id BIGINT PRIMARY KEY, name VARCHAR(150), status VARCHAR(20), latitude DOUBLE PRECISION, longitude DOUBLE PRECISION, address VARCHAR(255), phone VARCHAR(20));
CREATE TABLE customer_profiles (user_id BIGINT PRIMARY KEY REFERENCES users(id), td_cash_balance NUMERIC(12,2), latitude DOUBLE PRECISION, longitude DOUBLE PRECISION);
CREATE TABLE courier_profiles (user_id BIGINT PRIMARY KEY REFERENCES users(id), status VARCHAR(20), latitude DOUBLE PRECISION, longitude DOUBLE PRECISION);
CREATE TABLE orders (id BIGINT PRIMARY KEY, customer_id BIGINT, restaurant_id BIGINT, courier_id BIGINT, status VARCHAR(20), created_at TIMESTAMP WITH TIME ZONE, accepted_at TIMESTAMP WITH TIME ZONE, rejection_reason TEXT, total_price NUMERIC(10,2));
CREATE TABLE order_lines (id BIGINT PRIMARY KEY, order_id BIGINT REFERENCES orders(id), quantity INTEGER, unit_price NUMERIC(10,2));
CREATE TABLE tender_cash_transactions (id BIGINT PRIMARY KEY, customer_id BIGINT, order_id BIGINT, reason VARCHAR(24), tenders BIGINT);
INSERT INTO users VALUES
(1, 'Customer One', 'one@example.test', '5145550101', 'customer', 'secret hash'),
(2, 'Customer Two', 'two@example.test', NULL, 'customer', 'secret hash'),
(3, 'Idle Courier', 'idle@example.test', '5145550103', 'courier', 'secret hash'),
(4, 'Busy Courier', 'busy@example.test', NULL, 'courier', 'secret hash'),
(5, 'Offline Courier', 'offline@example.test', NULL, 'courier', 'secret hash'),
(6, 'Operator', 'operator@example.test', NULL, 'operator', 'secret hash'),
(7, 'Staff', 'staff@example.test', NULL, 'staff', 'secret hash'),
(8, 'New Courier', 'new@example.test', NULL, 'courier', 'secret hash');
INSERT INTO customer_profiles VALUES (1, 25.50, 45.5, -73.5);
INSERT INTO courier_profiles VALUES (3, 'idle', NULL, NULL), (4, 'carrying', 45.5, -73.5), (5, 'offline', NULL, NULL);
INSERT INTO restaurants VALUES (20, 'Second', 'paused', NULL, NULL, NULL, NULL), (10, 'First', 'open', 45.5, -73.5, 'Main Street', '5145550100');
INSERT INTO orders VALUES
(30, NULL, NULL, NULL, 'PLACED', NULL, NULL, NULL, NULL),
(10, 1, 10, 3, 'PLACED', TIMESTAMP WITH TIME ZONE '2026-10-29 12:00:00+00', NULL, NULL, 99.00),
(20, 2, 20, NULL, 'REFUNDED', TIMESTAMP WITH TIME ZONE '2026-10-29 13:00:00+00', TIMESTAMP WITH TIME ZONE '2026-10-29 13:01:00+00', 'Refunded', 12.50);
INSERT INTO order_lines VALUES (1, 10, 2, 10.00), (2, 10, 1, 5.00);
