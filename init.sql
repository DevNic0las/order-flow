-- Cria os schemas usados pelos servicos do order-flow
-- (os modulos tambem criam seus schemas via Flyway: CREATE SCHEMA IF NOT EXISTS)
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS email_verification;
