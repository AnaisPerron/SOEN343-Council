-- Demo seed for REG and ORD. Requires the existing database schema.
-- WARNING: Deletes and replaces all data in the 12 tables below.
-- Run ONLY against a demo database.
-- TRUNCATE RESTART IDENTITY ensures consistent data on every run.
-- Executed in a transaction; errors roll back all changes.
--
-- demo password: DemoPass123! (BCrypt-hashed, demo use only).
-- Restaurant accounts use the 'staff' role; ensure application consistency.
-- Authentication, permissions, order transitions, and FIFO queues
-- must be enforced by the application, not the seed.

BEGIN;

TRUNCATE TABLE
    order_line_options,
    order_events,
    order_lines,
    orders,
    menu_item_options,
    menu_items,
    menus,
    restaurant_staff,
    courier_profiles,
    customer_profiles,
    restaurants,
    users
RESTART IDENTITY;

-- ---------------- REG: USERS ----------------
-- IDs will be 1..9 in listed order thanks to RESTART IDENTITY
-- Fictional demo phone numbers satisfy the required users.phone column
INSERT INTO users (role, name, email, password_hash, phone) VALUES
('customer', 'Alice Martin', 'alice@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550101'), -- 1
('customer', 'Omar Hassan', 'omar@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550102'), -- 2
('customer', 'Maya Chen', 'maya@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550103'), -- 3
('staff', 'Sam Pizza Staff', 'sam@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550104'), -- 4
('staff', 'Rina Bowl Staff', 'rina@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550105'), -- 5
('courier', 'Leo Courier', 'leo@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550106'), -- 6
('courier', 'Nora Courier', 'nora@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550107'), -- 7
('operator', 'Olivia Operator', 'operator@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550108'), -- 8
('staff', 'Casey Closed Staff', 'casey@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550109'); -- 9

-- demo coordinates are illustrative points around Montreal (lat, lon)
INSERT INTO customer_profiles (user_id, td_cash_balance, latitude, longitude) VALUES
(1, 75.00, 45.5080, -73.5710),
(2, 40.00, 45.5140, -73.5620),
(3, 22.50, 45.4970, -73.5800);

INSERT INTO courier_profiles (user_id, latitude, longitude, status) VALUES
(6, 45.5090, -73.5730, 'idle'),
(7, 45.5180, -73.5610, 'offline');

-- ---------------- ORD: RESTAURANTS / MENUS ----------------
-- Open = can order; paused/closed = reject new orders
INSERT INTO restaurants (name, status, latitude, longitude) VALUES
('Metro Pizza', 'open', 45.5100, -73.5700), -- 1
('Green Bowl', 'paused', 45.5150, -73.5600), -- 2
('Night Noodles', 'closed', 45.5010, -73.5840); -- 3

INSERT INTO restaurant_staff (user_id, restaurant_id) VALUES
(4, 1), (5, 2), (9, 3);

INSERT INTO menus (restaurant_id, name) VALUES
(1, 'Metro Pizza Menu'), -- 1
(2, 'Green Bowl Menu'), -- 2
(3, 'Night Noodles Menu'); -- 3

-- The sold-out pizza is intentionally available for negative-case testing
INSERT INTO menu_items (menu_id, name, price, status, photo_url) VALUES
(1, 'Margherita Pizza', 14.00, 'available', NULL), -- 1
(1, 'Pepperoni Pizza', 17.00, 'available', NULL), -- 2
(1, 'Truffle Pizza', 22.00, 'sold_out', NULL), -- 3
(1, 'Sparkling Water', 3.00, 'available', NULL), -- 4
(2, 'Tofu Power Bowl', 15.00, 'available', NULL), -- 5
(2, 'Roasted Veggie Bowl', 13.00, 'available', NULL), -- 6
(3, 'Miso Noodles', 16.00, 'available', NULL); -- 7

INSERT INTO menu_item_options (menu_item_id, name, additional_cost) VALUES
(1, 'Extra basil', 1.00), -- 1
(1, 'Extra cheese', 2.00), -- 2
(2, 'Extra pepperoni', 3.00), -- 3
(5, 'Extra tofu', 2.50), -- 4
(6, 'Extra dressing', 0.50); -- 5

-- ---------------- ORD: EXISTING ORDERS ----------------
-- Several status snapshots to show active queues, order history and events.
-- All active orders belong to Metro Pizza (open); the paused restaurant has
-- a historical completed order to show that pausing does not erase history
INSERT INTO orders
(customer_id, restaurant_id, courier_id, status, created_at, accepted_at, rejection_reason)
VALUES
(1, 1, NULL, 'placed',    CURRENT_TIMESTAMP - INTERVAL '5 minutes',  NULL, NULL), -- 1 waiting for acceptance
(2, 1, NULL, 'accepted',  CURRENT_TIMESTAMP - INTERVAL '18 minutes', CURRENT_TIMESTAMP - INTERVAL '17 minutes', NULL), -- 2 FIFO first
(3, 1, NULL, 'accepted',  CURRENT_TIMESTAMP - INTERVAL '12 minutes', CURRENT_TIMESTAMP - INTERVAL '11 minutes', NULL), -- 3 FIFO second
(1, 1, NULL, 'preparing', CURRENT_TIMESTAMP - INTERVAL '35 minutes', CURRENT_TIMESTAMP - INTERVAL '34 minutes', NULL), -- 4 in progress
(2, 1, NULL, 'ready',     CURRENT_TIMESTAMP - INTERVAL '55 minutes', CURRENT_TIMESTAMP - INTERVAL '54 minutes', NULL), -- 5 ready, no courier assigned
(3, 1, NULL, 'rejected',  CURRENT_TIMESTAMP - INTERVAL '70 minutes', NULL, 'Kitchen unable to fulfill request'), -- 6
(1, 2, 7,    'delivered', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours 58 minutes', NULL), -- 7 historical
(2, 1, NULL, 'cancelled', CURRENT_TIMESTAMP - INTERVAL '90 minutes', NULL, NULL); -- 8

-- Price snapshots are independent of future changes to menu pricing
INSERT INTO order_lines (order_id, menu_item_id, quantity, unit_price) VALUES
(1, 1, 1, 14.00), -- 1
(1, 4, 1,  3.00), -- 2
(2, 2, 2, 17.00), -- 3
(3, 1, 1, 14.00), -- 4
(4, 2, 1, 17.00), -- 5
(5, 1, 1, 14.00), -- 6
(6, 2, 1, 17.00), -- 7
(7, 5, 1, 15.00), -- 8
(8, 4, 2,  3.00); -- 9

INSERT INTO order_line_options
(order_line_id, menu_item_id, menu_item_option_id, additional_cost) VALUES
(1, 1, 1, 1.00), -- Alice's Margherita with extra basil
(3, 2, 3, 3.00), -- Omar's Pepperoni with extra pepperoni
(4, 1, 2, 2.00), -- Maya's Margherita with extra cheese
(8, 5, 4, 2.50); -- Delivered tofu bowl with extra tofu

-- Historical status events with distinct event IDs assigned automatically
-- tiimestamps intentionally reflect the status progression for each order
INSERT INTO order_events
(order_id, previous_status, new_status, event_type, created_at) VALUES
(1, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '5 minutes'),
(2, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '18 minutes'),
(2, 'placed', 'accepted', 'order_accepted', CURRENT_TIMESTAMP - INTERVAL '17 minutes'),
(3, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '12 minutes'),
(3, 'placed', 'accepted', 'order_accepted', CURRENT_TIMESTAMP - INTERVAL '11 minutes'),
(4, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '35 minutes'),
(4, 'placed', 'accepted', 'order_accepted', CURRENT_TIMESTAMP - INTERVAL '34 minutes'),
(4, 'accepted', 'preparing', 'order_preparing', CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
(5, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '55 minutes'),
(5, 'placed', 'accepted', 'order_accepted', CURRENT_TIMESTAMP - INTERVAL '54 minutes'),
(5, 'accepted', 'preparing', 'order_preparing', CURRENT_TIMESTAMP - INTERVAL '50 minutes'),
(5, 'preparing', 'ready', 'order_ready', CURRENT_TIMESTAMP - INTERVAL '46 minutes'),
(6, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '70 minutes'),
(6, 'placed', 'rejected', 'order_rejected', CURRENT_TIMESTAMP - INTERVAL '68 minutes'),
(7, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
(7, 'placed', 'accepted', 'order_accepted', CURRENT_TIMESTAMP - INTERVAL '2 hours 58 minutes'),
(7, 'accepted', 'preparing', 'order_preparing', CURRENT_TIMESTAMP - INTERVAL '2 hours 50 minutes'),
(7, 'preparing', 'ready', 'order_ready', CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes'),
(7, 'ready', 'picked_up', 'order_picked_up', CURRENT_TIMESTAMP - INTERVAL '2 hours 30 minutes'),
(7, 'picked_up', 'delivered', 'order_delivered', CURRENT_TIMESTAMP - INTERVAL '2 hours 10 minutes'),
(8, NULL, 'placed', 'order_placed', CURRENT_TIMESTAMP - INTERVAL '90 minutes'),
(8, 'placed', 'cancelled', 'order_cancelled', CURRENT_TIMESTAMP - INTERVAL '88 minutes');

COMMIT;

-- Check initial records after seeding:
-- SELECT id, role, email FROM users ORDER BY id;
-- SELECT id, name, status FROM restaurants ORDER BY id;
-- SELECT id, restaurant_id, status, created_at FROM orders ORDER BY id;
-- SELECT id, order_id, previous_status, new_status FROM order_events ORDER BY id;
