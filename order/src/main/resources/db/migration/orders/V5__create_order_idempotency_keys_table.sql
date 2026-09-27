CREATE TABLE IF NOT EXISTS orders.tb_order_idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_order_idempotency_keys_order_id ON orders.tb_order_idempotency_keys (order_id);
