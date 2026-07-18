-- Users table (SCHEMA.md §2.1) with case-insensitive email uniqueness.
CREATE TABLE users (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         varchar(255) NOT NULL,
    password_hash varchar(100) NOT NULL,
    display_name  varchar(100) NOT NULL,
    role          varchar(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    active        boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email_lower ON users (lower(email));
