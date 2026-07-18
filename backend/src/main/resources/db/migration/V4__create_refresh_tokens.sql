-- Refresh token store (SCHEMA.md §2.6). Only the SHA-256 hash of a token is
-- persisted — the raw value never touches the database (RULES.md §29).
CREATE TABLE refresh_tokens (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    bigint      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz
);

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
