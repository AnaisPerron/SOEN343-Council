-- Restore demo image URLs without reseeding or changing other application data.
BEGIN;

UPDATE menu_items SET photo_url = REPLACE(photo_url,
    'http://localhost:8080/menu-items-imgs/', '/menu-items-imgs/')
WHERE photo_url LIKE 'http://localhost:8080/menu-items-imgs/%';

UPDATE restaurants restaurant SET cover_url = image.url
FROM (VALUES
    ('Maple & Main', '/menu-items-imgs/maple-glazed-chicken-sandwich.jpg'),
    ('Green Bowl Kitchen', '/menu-items-imgs/harvest-grain-bowl.jpg'),
    ('Cafe Nord', '/menu-items-imgs/smoked-salmon-bagel.jpg'),
    ('Little Italy Pizza', '/menu-items-imgs/margherita-pizza.jpg')
) AS image(name, url)
WHERE restaurant.name = image.name AND NULLIF(TRIM(restaurant.cover_url), '') IS NULL;

COMMIT;
