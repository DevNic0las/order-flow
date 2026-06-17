CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.orders
(
    id            BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    product_id    BIGINT       NOT NULL,
    quantity      INTEGER      NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_status ON orders.orders (status);