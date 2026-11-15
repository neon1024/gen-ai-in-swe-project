CREATE TABLE verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash  varchar(256) NOT NULL UNIQUE,
    user_id     UUID NOT NULL,
    CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz
);

CREATE INDEX idx_verification_tokens_user_id
    ON verification_tokens(user_id);
