CREATE SCHEMA IF NOT EXISTS email_verification;

CREATE TABLE email_verification.tb_email_verification (
                                                          id BIGSERIAL PRIMARY KEY,
                                                          user_id BIGINT NOT NULL UNIQUE
                                                              REFERENCES users.tb_users(id) ON DELETE CASCADE,
                                                          verification_code VARCHAR(6) NOT NULL,
                                                          expiration_at TIMESTAMP NOT NULL,
                                                          verified BOOLEAN NOT NULL DEFAULT FALSE
);