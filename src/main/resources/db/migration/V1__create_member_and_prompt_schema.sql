CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nickname VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE social_identity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_social_identity_provider_user UNIQUE (provider, provider_user_id),
    INDEX idx_social_identity_member_id (member_id)
);

CREATE TABLE prompt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    provenance VARCHAR(20) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    owner_member_id BIGINT,
    source_name VARCHAR(255),
    source_url VARCHAR(2048),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_prompt_owner_member_id (owner_member_id)
);

CREATE TABLE prompt_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    prompt_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    active_category VARCHAR(50) GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN category ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_prompt_category_active UNIQUE (prompt_id, active_category),
    INDEX idx_prompt_category_category_prompt_id (category, prompt_id)
);

CREATE TABLE prompt_like (
    id BIGINT NOT NULL AUTO_INCREMENT,
    prompt_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    active_member_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN member_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_prompt_like_active UNIQUE (prompt_id, active_member_id),
    INDEX idx_prompt_like_member_id_prompt_id (member_id, prompt_id),
    INDEX idx_prompt_like_created_at_prompt_id (created_at, prompt_id)
);
