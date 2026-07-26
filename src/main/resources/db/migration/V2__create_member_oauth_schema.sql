CREATE TABLE oauth_login_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    state_hash CHAR(64) NOT NULL,
    encrypted_pkce_verifier VARCHAR(1024) NOT NULL,
    nonce_hash CHAR(64),
    return_path VARCHAR(2048) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_login_attempt_state_hash UNIQUE (state_hash),
    INDEX idx_oauth_login_attempt_expires_at (expires_at)
);

CREATE TABLE refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    family_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    predecessor_id BIGINT,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    reused_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_token_hash UNIQUE (token_hash),
    INDEX idx_refresh_token_member_id (member_id),
    INDEX idx_refresh_token_family_id (family_id)
);
