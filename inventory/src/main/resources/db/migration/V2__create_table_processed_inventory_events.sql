CREATE TABLE inventory.tb_processed_inventory_events (
                                                         id BIGSERIAL PRIMARY KEY,
                                                         event_id UUID NOT NULL UNIQUE,
                                                         processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);