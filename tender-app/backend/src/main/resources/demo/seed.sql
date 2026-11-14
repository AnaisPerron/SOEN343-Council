-- Demo baseline for REG, ORD, DLV, and PRC. Requires the existing database schema.
-- warning deletes and replaces all data in the twenty tables below
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
    ledger_entries,
    bill,
    tender_cash_transactions,
    assignments,
    auth_sessions,
    operator_profiles,
    order_items,
    order_line_options,
    order_events,
    order_lines,
    orders,
    menu_photos,
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
('staff', 'Maple & Main', 'sam@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550104'), -- 4
('staff', 'Green Bowl Kitchen', 'rina@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550105'), -- 5
('courier', 'Cafe Nord', 'leo@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550106'), -- 6
('courier', 'Nora Courier', 'nora@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550107'), -- 7
('operator', 'Olivia Operator', 'operator@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550108'), -- 8
('staff', 'Casey Closed Staff', 'casey@demo.test', '$2y$10$S6fasfa9HK9ZwYkQF0rt/OSV5PIm/eUDvHtHuKu.dR4QdoWbo3dfi', '+15145550109'); -- 9

-- demo coordinates are illustrative points around Montreal (lat, lon)
INSERT INTO customer_profiles (user_id, td_cash_balance, address, latitude, longitude) VALUES
(1, 75.00, '1250 Rue Peel, Montreal, Canada', 45.5080, -73.5710),
(2, 40.00, '275 Rue Sherbrooke E, Montreal, Canada', 45.5140, -73.5620),
(3, 22.50, '1455 Boul. De Maisonneuve O, Montreal, Canada', 45.4970, -73.5800);

INSERT INTO operator_profiles (user_id) VALUES
(8);

INSERT INTO courier_profiles (user_id, latitude, longitude, status) VALUES
(6, 45.5090, -73.5730, 'idle'),
(7, 45.5180, -73.5610, 'offline');

-- ---------------- ORD: RESTAURANTS / MENUS ----------------
-- Open = can order; paused/closed = reject new orders
INSERT INTO restaurants (name, status, latitude, longitude, cover_url) VALUES
('Maple & Main', 'open', 45.5100, -73.5700, '/menu-items-imgs/maple-glazed-chicken-sandwich.jpg'), -- 1
('Green Bowl Kitchen', 'paused', 45.5150, -73.5600, '/menu-items-imgs/harvest-grain-bowl.jpg'), -- 2
('Cafe Nord', 'closed', 45.5010, -73.5840, '/menu-items-imgs/smoked-salmon-bagel.jpg'), -- 3
('Little Italy Pizza', 'open', 45.5352, -73.6132, '/menu-items-imgs/margherita-pizza.jpg'); -- 4

INSERT INTO restaurant_staff (user_id, restaurant_id) VALUES
(4, 1), (5, 2), (9, 3);

INSERT INTO menus (restaurant_id) VALUES
(1),
(2),
(3),
(4);

-- menu photos are served by the backend
-- The sold-out crumble is intentionally included for negative-case testing.
INSERT INTO menu_items (menu_id, name, price, status, photo_url) VALUES
(1, 'Maple-glazed chicken sandwich', 14.50, 'available', '/menu-items-imgs/maple-glazed-chicken-sandwich.jpg'), -- 1
(1, 'Truffle fries', 6.25, 'available', '/menu-items-imgs/truffle.jpg'), -- 2
(1, 'Apple crumble', 7.00, 'sold_out', '/menu-items-imgs/apple-crumble.jpg'), -- 3
(2, 'Harvest grain bowl', 15.75, 'available', '/menu-items-imgs/harvest-grain-bowl.jpg'), -- 4
(2, 'Miso tofu bowl', 14.00, 'available', '/menu-items-imgs/miso-tofu-bowl.jpg'), -- 5
(3, 'Smoked salmon bagel', 12.50, 'available', '/menu-items-imgs/smoked-salmon-bagel.jpg'), -- 6
(4, 'Margherita pizza', 17.00, 'available', '/menu-items-imgs/margherita-pizza.jpg'); -- 7

UPDATE menu_items SET category = CASE id
    WHEN 1 THEN 'Popular' WHEN 2 THEN 'Sides' WHEN 3 THEN 'Desserts'
    WHEN 4 THEN 'Bowls' WHEN 5 THEN 'Bowls' WHEN 6 THEN 'Popular'
    WHEN 7 THEN 'Pizza' END,
    description = CASE id
    WHEN 1 THEN 'Tender chicken, maple glaze and crisp greens.'
    WHEN 2 THEN 'Golden fries tossed with fragrant truffle.'
    WHEN 3 THEN 'Warm apples under a buttery crumble.'
    WHEN 4 THEN 'Seasonal vegetables, grains and house dressing.'
    WHEN 5 THEN 'Miso tofu with fresh vegetables and rice.'
    WHEN 6 THEN 'Smoked salmon, cream cheese and a toasted bagel.'
    WHEN 7 THEN 'Tomato, mozzarella and fresh basil.' END;

UPDATE restaurants SET opening_hour = TIME '09:00',
    closing_hour = CASE WHEN id IN (1, 4) THEN TIME '22:00' ELSE TIME '17:00' END;

INSERT INTO menu_item_options (menu_item_id, name, additional_cost) VALUES
(1, 'Add avocado', 2.00), -- 1
(1, 'Gluten-free bun', 1.50), -- 2
(2, 'Extra truffle seasoning', 1.00), -- 3
(5, 'Extra tofu', 2.50), -- 4
(4, 'Add grilled chicken', 4.00), -- 5
(7, 'Extra mozzarella', 2.50); -- 6

-- ---------------- ORD: EXISTING ORDERS ----------------
-- Several status snapshots to show active queues, order history and events.
-- All active orders belong to Maple & Main (open); the paused restaurant has
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
(1, 1, 1, 14.50), -- 1
(1, 2, 1,  6.25), -- 2
(2, 2, 2,  6.25), -- 3
(3, 1, 1, 14.50), -- 4
(4, 2, 1,  6.25), -- 5
(5, 1, 1, 14.50), -- 6
(6, 2, 1,  6.25), -- 7
(7, 5, 1, 14.00), -- 8
(8, 2, 2,  6.25); -- 9

INSERT INTO order_line_options
(order_line_id, menu_item_id, menu_item_option_id, additional_cost) VALUES
(1, 1, 1, 2.00), -- Alice's sandwich with avocado
(3, 2, 3, 1.00), -- Omar's fries with extra truffle seasoning
(4, 1, 2, 1.50), -- Maya's sandwich with a gluten-free bun
(8, 5, 4, 2.50); -- Delivered tofu bowl with extra tofu

-- Historical status events with distinct event IDs assigned automatically
-- timestamps intentionally reflect the status progression for each order
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

-- Persist snapshots used by current order history, delivery, and pricing screens.
UPDATE order_lines SET item_snapshot = menu_items.name
FROM menu_items WHERE menu_items.id = order_lines.menu_item_id;

UPDATE orders SET
    total_price = (SELECT SUM(line.quantity * (line.unit_price + COALESCE(
        (SELECT SUM(opt.additional_cost) FROM order_line_options opt
         WHERE opt.order_line_id = line.id), 0)))
        FROM order_lines line WHERE line.order_id = orders.id),
    customer_name = profile_user.name,
    customer_phone = profile_user.phone,
    delivery_address = profile.address,
    delivery_latitude = profile.latitude,
    delivery_longitude = profile.longitude,
    status_changed_at = (SELECT MAX(event.created_at) FROM order_events event
                         WHERE event.order_id = orders.id)
FROM customer_profiles profile JOIN users profile_user ON profile_user.id = profile.user_id
WHERE profile.user_id = orders.customer_id;

UPDATE orders SET pickup_date = created_at + INTERVAL '30 minutes',
    date_delivered = created_at + INTERVAL '50 minutes'
WHERE id = 7;

-- Historical demo orders retain their original totals with no added fees.
INSERT INTO bill (order_id, customer_id, amount, date, service_fee, delivery_fee, small_order_fee)
SELECT id, customer_id, total_price, created_at, 0, 0, 0 FROM orders ORDER BY id;

INSERT INTO ledger_entries (bill_id, transaction_type, timestamp, final_order_status, total_charge)
SELECT bill.id, CASE WHEN orders.status = 'rejected' THEN 'Refund' ELSE 'Payment' END,
    orders.status_changed_at, orders.status, orders.total_price
FROM bill JOIN orders ON orders.id = bill.order_id ORDER BY orders.id;

COMMIT;

-- Check initial records after seeding:
-- SELECT id, role, email FROM users ORDER BY id;
-- SELECT id, name, status FROM restaurants ORDER BY id;
-- SELECT id, restaurant_id, status, created_at FROM orders ORDER BY id;
-- SELECT id, order_id, previous_status, new_status FROM order_events ORDER BY id;
