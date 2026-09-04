DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_user_account_access_user_account'
          AND conrelid = 'user_account_access'::regclass
          AND contype = 'u'
    ) THEN
        ALTER TABLE user_account_access
            ADD CONSTRAINT uk_user_account_access_user_account UNIQUE (user_id, account_id);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS user_account_pins (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    pin_lookup_digest VARCHAR(43),
    online_pin_hash VARCHAR(512),
    encrypted_offline_verifier TEXT,
    offline_verifier_nonce VARCHAR(32),
    encryption_key_version INTEGER,
    pin_length INTEGER,
    status VARCHAR(16) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    lockout_level INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_failed_at TIMESTAMPTZ,
    credential_version BIGINT NOT NULL DEFAULT 1,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    entity_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_account_pins_account_user UNIQUE (account_id, user_id),
    CONSTRAINT uk_user_account_pins_account_digest UNIQUE (account_id, pin_lookup_digest),
    CONSTRAINT ck_user_account_pins_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_user_account_pins_length CHECK (pin_length IN (4, 6))
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_user_account_pins_status'
          AND conrelid = 'user_account_pins'::regclass
    ) THEN
        ALTER TABLE user_account_pins
            ADD CONSTRAINT ck_user_account_pins_status CHECK (status IN ('ACTIVE', 'REVOKED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_user_account_pins_length'
          AND conrelid = 'user_account_pins'::regclass
    ) THEN
        ALTER TABLE user_account_pins
            ADD CONSTRAINT ck_user_account_pins_length CHECK (pin_length IN (4, 6));
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS ix_user_account_pins_account_status
    ON user_account_pins(account_id, status);

CREATE TABLE IF NOT EXISTS ipad_devices (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    location_id UUID NOT NULL REFERENCES locations(id),
    device_name VARCHAR(255) NOT NULL,
    device_token_hash VARCHAR(43) NOT NULL,
    device_public_key TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    enrolled_at TIMESTAMPTZ NOT NULL,
    enrolled_by UUID NOT NULL,
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by UUID,
    bundle_version BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ipad_devices_token_hash ON ipad_devices(device_token_hash);
CREATE INDEX IF NOT EXISTS ix_ipad_devices_account_location ON ipad_devices(account_id, location_id);

CREATE TABLE IF NOT EXISTS pin_authentication_audit (
    id UUID PRIMARY KEY,
    event_type VARCHAR(48) NOT NULL,
    source_event_id UUID UNIQUE,
    sequence_number BIGINT,
    account_id UUID NOT NULL REFERENCES accounts(id),
    location_id UUID,
    target_user_id UUID,
    actor_user_id UUID,
    actor_type VARCHAR(16) NOT NULL,
    device_id UUID,
    line_check_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    correlation_id VARCHAR(128),
    failure_category VARCHAR(64),
    lockout_duration_seconds BIGINT,
    lockout_until TIMESTAMPTZ,
    credential_version BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pin_audit_source_event
    ON pin_authentication_audit(source_event_id);
CREATE INDEX IF NOT EXISTS ix_pin_audit_account_occurred
    ON pin_authentication_audit(account_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_pin_audit_device_sequence
    ON pin_authentication_audit(device_id, sequence_number);

CREATE TABLE IF NOT EXISTS pin_auth_throttles (
    id UUID PRIMARY KEY,
    scope_type VARCHAR(16) NOT NULL,
    account_id UUID NOT NULL REFERENCES accounts(id),
    ip_address VARCHAR(64) NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL,
    failed_count INTEGER NOT NULL DEFAULT 0,
    blocked_until TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_pin_auth_throttle_scope UNIQUE (scope_type, account_id, ip_address)
);

ALTER TABLE line_checks
    ADD COLUMN IF NOT EXISTS auth_device_id UUID,
    ADD COLUMN IF NOT EXISTS auth_account_id UUID,
    ADD COLUMN IF NOT EXISTS auth_location_id UUID,
    ADD COLUMN IF NOT EXISTS auth_user_id UUID,
    ADD COLUMN IF NOT EXISTS auth_credential_version BIGINT,
    ADD COLUMN IF NOT EXISTS auth_verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS auth_local_event_id UUID,
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(32);

CREATE UNIQUE INDEX IF NOT EXISTS uk_line_checks_auth_local_event
    ON line_checks(auth_local_event_id)
    WHERE auth_local_event_id IS NOT NULL;
