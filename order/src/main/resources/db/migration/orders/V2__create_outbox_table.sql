CREATE TABLE IF NOT EXISTS orders.tb_outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    published_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created_at ON orders.tb_outbox_events (status, created_at);
