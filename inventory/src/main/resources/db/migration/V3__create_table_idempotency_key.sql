
CREATE TABLE IF NOT EXISTS inventory.tb_inventory_idempotency_keys (
                                                                id BIGSERIAL PRIMARY KEY,
                                                                idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_inventory_idempotency_keys_product_id ON inventory.tb_inventory_idempotency_keys (product_id);
