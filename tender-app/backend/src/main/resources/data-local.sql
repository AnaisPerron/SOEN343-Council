INSERT INTO health_check (id, status) VALUES (1, 'OK');

INSERT INTO menu_items (name, price) VALUES ('Pasta', 12.50);
INSERT INTO menu_items (name, price) VALUES ('Garden salad', 8.00);
INSERT INTO menu_items (name, price) VALUES ('Tomato soup', 6.50);

INSERT INTO orders (restaurant_id, created_at, status, total_price)
VALUES (1, CURRENT_TIMESTAMP, 'PLACED', 25.00);
INSERT INTO orders (restaurant_id, created_at, status, accepted_at, total_price)
VALUES (1, CURRENT_TIMESTAMP, 'PREPARING', CURRENT_TIMESTAMP, 16.00);
INSERT INTO orders (restaurant_id, created_at, status, accepted_at, total_price)
VALUES (1, CURRENT_TIMESTAMP, 'PREPARING', CURRENT_TIMESTAMP, 13.00);

INSERT INTO order_lines (order_id, menu_item_id, quantity, unit_price)
SELECT o.id, m.id, 2, m.price
FROM orders o
JOIN menu_items m ON m.name = 'Pasta'
WHERE o.restaurant_id = 1 AND o.status = 'PLACED';

INSERT INTO order_lines (order_id, menu_item_id, quantity, unit_price)
SELECT o.id, m.id, 2, m.price
FROM orders o
JOIN menu_items m ON m.name = 'Garden salad'
WHERE o.restaurant_id = 1 AND o.status = 'PREPARING' AND o.total_price = 16.00;

INSERT INTO order_lines (order_id, menu_item_id, quantity, unit_price)
SELECT o.id, m.id, 2, m.price
FROM orders o
JOIN menu_items m ON m.name = 'Tomato soup'
WHERE o.restaurant_id = 1 AND o.status = 'PREPARING' AND o.total_price = 13.00;
