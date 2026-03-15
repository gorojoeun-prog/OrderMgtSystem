INSERT INTO products (id, name, price)
VALUES
    (1, 'Americano', 3500.00),
    (2, 'Latte', 4500.00),
    (3, 'Sandwich', 6500.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO stocks (product_id, quantity)
VALUES
    (1, 100),
    (2, 80),
    (3, 50)
ON CONFLICT (product_id) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('products', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM products),
    true
);

SELECT setval(
    pg_get_serial_sequence('stocks', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM stocks),
    true
);
