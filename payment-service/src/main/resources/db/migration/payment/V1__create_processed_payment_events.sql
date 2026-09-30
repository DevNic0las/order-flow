CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE payment.tb_processed_payment_events
(
    id           BIGSERIAL PRIMARY KEY,
    event_id     UUID NOT NULL UNIQUE,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
