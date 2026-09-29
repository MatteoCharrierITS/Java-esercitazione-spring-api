CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    utente_id BIGINT NOT NULL REFERENCES utenti(id),
    refresh_token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_sessions_user ON auth_sessions(utente_id);
