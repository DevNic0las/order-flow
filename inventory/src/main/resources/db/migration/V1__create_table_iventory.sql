CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.tb_inventory
(
    id            BIGSERIAL PRIMARY KEY,
    product_name    VARCHAR(255) NOT NULL,
    quantity      INTEGER      NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);