CREATE TABLE inventory.tb_processed_payment_compensation_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     UUID NOT NULL UNIQUE,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
