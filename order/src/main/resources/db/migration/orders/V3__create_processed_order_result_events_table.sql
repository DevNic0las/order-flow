CREATE TABLE IF NOT EXISTS orders.tb_processed_order_result_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    processed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_processed_order_result_events_event_id ON orders.tb_processed_order_result_events (event_id);
