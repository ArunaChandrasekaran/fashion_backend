-- Direct PostgreSQL seed for local demo customers and orders.
-- Password for every demo customer: CustomerTest!23
-- Run: psql -U postgres -d fashion -f backend/src/main/resources/db/seed/demo_data.sql

INSERT INTO users (full_name, email, phone, password_hash, enabled)
SELECT v.full_name, v.email, v.phone, v.password_hash, TRUE
FROM (VALUES
    ('Aanya Kapoor', 'aanya@fashion.test', '9876543210', '$2a$10$.m44n3yrCwIcN63QlDKFiuD.IcGYRP90yQ8R.8EVzg./gazTZC38K'),
    ('Meera Shah', 'meera@fashion.test', '9876501234', '$2a$10$.m44n3yrCwIcN63QlDKFiuD.IcGYRP90yQ8R.8EVzg./gazTZC38K'),
    ('Rhea Menon', 'rhea@fashion.test', '9845011122', '$2a$10$.m44n3yrCwIcN63QlDKFiuD.IcGYRP90yQ8R.8EVzg./gazTZC38K'),
    ('Kabir Malhotra', 'kabir@fashion.test', '9810012345', '$2a$10$.m44n3yrCwIcN63QlDKFiuD.IcGYRP90yQ8R.8EVzg./gazTZC38K'),
    ('Ishita Rao', 'ishita@fashion.test', '9000012345', '$2a$10$.m44n3yrCwIcN63QlDKFiuD.IcGYRP90yQ8R.8EVzg./gazTZC38K')
) AS v(full_name, email, phone, password_hash)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'USER'
WHERE u.email IN (
    'aanya@fashion.test', 'meera@fashion.test', 'rhea@fashion.test',
    'kabir@fashion.test', 'ishita@fashion.test'
)
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

INSERT INTO addresses (user_id, full_name, phone, line1, line2, city, state, postal_code, country, is_default)
SELECT u.id, v.full_name, v.phone, v.line1, v.line2, v.city, v.state, v.postal_code, 'India', TRUE
FROM (VALUES
    ('aanya@fashion.test', 'Aanya Kapoor', '9876543210', '14 Atelier Lane', 'Bandra West', 'Mumbai', 'Maharashtra', '400050'),
    ('meera@fashion.test', 'Meera Shah', '9876501234', '22 Riverfront Road', 'Navrangpura', 'Ahmedabad', 'Gujarat', '380009'),
    ('rhea@fashion.test', 'Rhea Menon', '9845011122', '8 Lavelle Road', 'Ashok Nagar', 'Bengaluru', 'Karnataka', '560001'),
    ('kabir@fashion.test', 'Kabir Malhotra', '9810012345', '31 Khan Market', 'Golf Links', 'New Delhi', 'Delhi', '110003'),
    ('ishita@fashion.test', 'Ishita Rao', '9000012345', '5 Banjara Hills Road No. 12', 'Banjara Hills', 'Hyderabad', 'Telangana', '500034')
) AS v(email, full_name, phone, line1, line2, city, state, postal_code)
JOIN users u ON u.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM addresses a WHERE a.user_id = u.id);

INSERT INTO orders (
    order_number, user_id, status, payment_method, payment_status,
    ship_full_name, ship_phone, ship_line1, ship_line2, ship_city, ship_state, ship_postal_code, ship_country,
    subtotal, discount_amount, shipping_charge, final_total, created_at, updated_at
)
SELECT v.order_number, u.id, v.status, 'CASH_ON_DELIVERY', v.payment_status,
       a.full_name, a.phone, a.line1, a.line2, a.city, a.state, a.postal_code, a.country,
       0, 0, 0, 0, v.created_at, v.created_at
FROM (VALUES
    ('FASH-DEMO-001', 'aanya@fashion.test', 'DELIVERED', 'PAID', TIMESTAMPTZ '2026-09-25 11:20:00+05:30'),
    ('FASH-DEMO-002', 'aanya@fashion.test', 'PLACED', 'PENDING', TIMESTAMPTZ '2026-10-12 09:40:00+05:30'),
    ('FASH-DEMO-003', 'meera@fashion.test', 'SHIPPED', 'PENDING', TIMESTAMPTZ '2026-10-07 16:10:00+05:30'),
    ('FASH-DEMO-004', 'meera@fashion.test', 'CANCELLED', 'REFUNDED', TIMESTAMPTZ '2026-10-01 13:05:00+05:30'),
    ('FASH-DEMO-005', 'rhea@fashion.test', 'PROCESSING', 'PENDING', TIMESTAMPTZ '2026-10-10 18:30:00+05:30'),
    ('FASH-DEMO-006', 'kabir@fashion.test', 'CONFIRMED', 'PENDING', TIMESTAMPTZ '2026-10-11 12:15:00+05:30'),
    ('FASH-DEMO-007', 'kabir@fashion.test', 'DELIVERED', 'PAID', TIMESTAMPTZ '2026-09-22 15:45:00+05:30'),
    ('FASH-DEMO-008', 'ishita@fashion.test', 'PLACED', 'PENDING', TIMESTAMPTZ '2026-10-13 08:50:00+05:30')
) AS v(order_number, email, status, payment_status, created_at)
JOIN users u ON u.email = v.email
JOIN addresses a ON a.user_id = u.id AND a.is_default = TRUE
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_number = v.order_number);

INSERT INTO order_items (
    order_id, product_id, variant_id, product_name, sku, size, color, image_url,
    unit_price, discount_percent, quantity, line_total
)
SELECT o.id, p.id, pv.id, p.name, pv.sku, pv.size, pv.color,
       (SELECT url FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.is_primary DESC, pi.sort_order LIMIT 1),
       p.price, p.discount_percent, v.quantity,
       ROUND(p.price * v.quantity * (1 - p.discount_percent / 100), 2)
FROM (VALUES
    ('FASH-DEMO-001', 'FASH-EV-001-M-BUR', 1),
    ('FASH-DEMO-001', 'FASH-CK-001-S-CHA', 1),
    ('FASH-DEMO-002', 'FASH-CA-001-M-BLK', 1),
    ('FASH-DEMO-003', 'FASH-PA-001-S-BLS', 1),
    ('FASH-DEMO-003', 'FASH-SU-001-M-NAV', 1),
    ('FASH-DEMO-004', 'FASH-MX-001-M-FOR', 1),
    ('FASH-DEMO-005', 'FASH-BR-001-S-IVY', 1),
    ('FASH-DEMO-006', 'FASH-EV-003-M-RBY', 1),
    ('FASH-DEMO-007', 'FASH-CK-002-M-GLD', 1),
    ('FASH-DEMO-007', 'FASH-MD-001-L-PLM', 1),
    ('FASH-DEMO-008', 'FASH-SU-003-M-RSE', 2),
    ('FASH-DEMO-008', 'FASH-CA-002-S-CRM', 1)
) AS v(order_number, variant_sku, quantity)
JOIN orders o ON o.order_number = v.order_number
JOIN product_variants pv ON pv.sku = v.variant_sku
JOIN products p ON p.id = pv.product_id
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi WHERE oi.order_id = o.id AND oi.sku = pv.sku
);

UPDATE orders o
SET subtotal = s.subtotal,
    discount_amount = s.discount_amount,
    shipping_charge = CASE WHEN s.subtotal - s.discount_amount >= 2499 THEN 0 ELSE 99 END,
    final_total = (s.subtotal - s.discount_amount)
                  + CASE WHEN s.subtotal - s.discount_amount >= 2499 THEN 0 ELSE 99 END
FROM (
    SELECT order_id,
           SUM(unit_price * quantity) AS subtotal,
           SUM(ROUND(unit_price * quantity * discount_percent / 100, 2)) AS discount_amount
    FROM order_items
    GROUP BY order_id
) s
WHERE o.id = s.order_id
  AND o.order_number LIKE 'FASH-DEMO-%';

INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, notes, created_at)
SELECT o.id, h.from_status, h.to_status, o.user_id, h.notes, o.created_at + (h.step || ' days')::INTERVAL
FROM orders o
JOIN (VALUES
    ('FASH-DEMO-001', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0),
    ('FASH-DEMO-001', 'PLACED', 'CONFIRMED', 'Confirmed', 1),
    ('FASH-DEMO-001', 'CONFIRMED', 'PROCESSING', 'Processing', 2),
    ('FASH-DEMO-001', 'PROCESSING', 'SHIPPED', 'Shipped', 4),
    ('FASH-DEMO-001', 'SHIPPED', 'DELIVERED', 'Delivered', 7),
    ('FASH-DEMO-002', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0),
    ('FASH-DEMO-003', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0),
    ('FASH-DEMO-003', 'PLACED', 'CONFIRMED', 'Confirmed', 1),
    ('FASH-DEMO-003', 'CONFIRMED', 'PROCESSING', 'Processing', 2),
    ('FASH-DEMO-003', 'PROCESSING', 'SHIPPED', 'Shipped', 3),
    ('FASH-DEMO-004', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0),
    ('FASH-DEMO-004', 'PLACED', 'CANCELLED', 'Cancelled', 1),
    ('FASH-DEMO-005', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0),
    ('FASH-DEMO-005', 'PLACED', 'CONFIRMED', 'Confirmed', 1),
    ('FASH-DEMO-005', 'CONFIRMED', 'PROCESSING', 'Processing', 2),
    ('FASH-DEMO-006', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0),
    ('FASH-DEMO-006', 'PLACED', 'CONFIRMED', 'Confirmed', 1),
    ('FASH-DEMO-007', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0),
    ('FASH-DEMO-007', 'PLACED', 'CONFIRMED', 'Confirmed', 1),
    ('FASH-DEMO-007', 'CONFIRMED', 'PROCESSING', 'Processing', 2),
    ('FASH-DEMO-007', 'PROCESSING', 'SHIPPED', 'Shipped', 4),
    ('FASH-DEMO-007', 'SHIPPED', 'DELIVERED', 'Delivered', 8),
    ('FASH-DEMO-008', CAST(NULL AS VARCHAR), 'PLACED', 'Order placed', 0)
) AS h(order_number, from_status, to_status, notes, step)
  ON h.order_number = o.order_number
WHERE NOT EXISTS (
    SELECT 1 FROM order_status_history x
    WHERE x.order_id = o.id AND x.to_status = h.to_status AND COALESCE(x.from_status, '') = COALESCE(h.from_status, '')
);

INSERT INTO payments (order_id, method, status, amount, provider)
SELECT o.id, 'CASH_ON_DELIVERY', o.payment_status, o.final_total, 'COD'
FROM orders o
WHERE o.order_number LIKE 'FASH-DEMO-%'
  AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id);
