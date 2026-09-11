CREATE TABLE IF NOT EXISTS user_eula_acceptances (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    eula_version INTEGER NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,
    device_id UUID,
    app_version VARCHAR(128),
    ip_address VARCHAR(128),
    CONSTRAINT uk_user_eula_acceptance_user_version UNIQUE (user_id, eula_version)
);

CREATE INDEX IF NOT EXISTS idx_user_eula_acceptances_user_id
    ON user_eula_acceptances(user_id);
