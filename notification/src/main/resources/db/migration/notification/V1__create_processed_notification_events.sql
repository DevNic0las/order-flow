CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.tb_processed_notification_events
(
    id           BIGSERIAL PRIMARY KEY,
    event_id     UUID NOT NULL UNIQUE,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
