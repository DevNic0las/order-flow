CREATE SCHEMA IF NOT EXISTS users;
CREATE TYPE user_role AS ENUM ('ADMIN', 'CUSTOMER');
CREATE TABLE users.tb_users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role  user_role NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);
