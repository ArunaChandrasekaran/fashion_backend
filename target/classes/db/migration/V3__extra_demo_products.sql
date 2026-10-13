INSERT INTO products (name, description, category_id, price, discount_percent, sku, status) VALUES
('Midnight Column Gown', 'A midnight silk column with a draped cowl and a narrow train. Fully lined for evening sittings.', 1, 23999.00, 12.00, 'FASH-EV-004', 'ACTIVE'),
('Sand Linen Wrap Dress', 'Stone linen wrap with a self belt and a midi hem. Cut for warm days and gallery visits.', 4, 6299.00, 8.00, 'FASH-CA-003', 'ACTIVE'),
('Pearl Cocktail Sheath', 'Ivory crepe sheath with pearl-finished buttons and a bateau neckline.', 2, 13999.00, 10.00, 'FASH-CK-003', 'ACTIVE');

INSERT INTO product_images (product_id, url, alt_text, sort_order, is_primary)
SELECT id, 'https://images.unsplash.com/photo-1502716119720-b23a93e5fe1b?auto=format&fit=crop&w=1400&q=80', 'Midnight column gown', 0, TRUE
FROM products WHERE sku = 'FASH-EV-004';

INSERT INTO product_images (product_id, url, alt_text, sort_order, is_primary)
SELECT id, 'https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=1400&q=80', 'Sand linen wrap dress', 0, TRUE
FROM products WHERE sku = 'FASH-CA-003';

INSERT INTO product_images (product_id, url, alt_text, sort_order, is_primary)
SELECT id, 'https://images.unsplash.com/photo-1566174053879-31528523f8ae?auto=format&fit=crop&w=1400&q=80', 'Pearl cocktail sheath', 0, TRUE
FROM products WHERE sku = 'FASH-CK-003';

INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'S', 'Midnight', 'FASH-EV-004-S-MID', 4, TRUE FROM products WHERE sku = 'FASH-EV-004';
INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'M', 'Midnight', 'FASH-EV-004-M-MID', 5, TRUE FROM products WHERE sku = 'FASH-EV-004';
INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'L', 'Midnight', 'FASH-EV-004-L-MID', 3, TRUE FROM products WHERE sku = 'FASH-EV-004';

INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'S', 'Sand', 'FASH-CA-003-S-SND', 8, TRUE FROM products WHERE sku = 'FASH-CA-003';
INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'M', 'Sand', 'FASH-CA-003-M-SND', 9, TRUE FROM products WHERE sku = 'FASH-CA-003';
INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'L', 'Sand', 'FASH-CA-003-L-SND', 7, TRUE FROM products WHERE sku = 'FASH-CA-003';

INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'S', 'Ivory', 'FASH-CK-003-S-IVY', 5, TRUE FROM products WHERE sku = 'FASH-CK-003';
INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'M', 'Ivory', 'FASH-CK-003-M-IVY', 6, TRUE FROM products WHERE sku = 'FASH-CK-003';
INSERT INTO product_variants (product_id, size, color, sku, stock_quantity, active)
SELECT id, 'L', 'Pearl', 'FASH-CK-003-L-PRL', 4, TRUE FROM products WHERE sku = 'FASH-CK-003';
