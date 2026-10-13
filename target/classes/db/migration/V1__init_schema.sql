CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES ('ADMIN'), ('USER');

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    slug VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories (id),
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (discount_percent >= 0 AND discount_percent <= 100),
    sku VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_name ON products (name);

CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(200),
    sort_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_product_images_product ON product_images (product_id);

CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    size VARCHAR(20) NOT NULL,
    color VARCHAR(40) NOT NULL,
    sku VARCHAR(64) NOT NULL UNIQUE,
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (product_id, size, color)
);

CREATE INDEX idx_variants_product ON product_variants (product_id);

CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants (id),
    quantity INT NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, variant_id)
);

CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    line1 VARCHAR(200) NOT NULL,
    line2 VARCHAR(200),
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(80) NOT NULL DEFAULT 'India',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_addresses_user ON addresses (user_id);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users (id),
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    ship_full_name VARCHAR(120) NOT NULL,
    ship_phone VARCHAR(20) NOT NULL,
    ship_line1 VARCHAR(200) NOT NULL,
    ship_line2 VARCHAR(200),
    ship_city VARCHAR(80) NOT NULL,
    ship_state VARCHAR(80) NOT NULL,
    ship_postal_code VARCHAR(20) NOT NULL,
    ship_country VARCHAR(80) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL,
    shipping_charge NUMERIC(12, 2) NOT NULL,
    final_total NUMERIC(12, 2) NOT NULL,
    idempotency_key VARCHAR(64) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created ON orders (created_at);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products (id) ON DELETE SET NULL,
    variant_id BIGINT REFERENCES product_variants (id) ON DELETE SET NULL,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    size VARCHAR(20) NOT NULL,
    color VARCHAR(40) NOT NULL,
    image_url VARCHAR(1000),
    unit_price NUMERIC(12, 2) NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL,
    quantity INT NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by BIGINT REFERENCES users (id),
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_history_order ON order_status_history (order_id);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    provider VARCHAR(40),
    provider_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
