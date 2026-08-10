CREATE TABLE prompt_collection (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    active_slug VARCHAR(100) GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN slug ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_prompt_collection_active_slug UNIQUE (active_slug)
);

CREATE TABLE prompt_collection_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    collection_id BIGINT NOT NULL,
    prompt_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    active_prompt_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN prompt_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_prompt_collection_item_active UNIQUE (collection_id, active_prompt_id),
    INDEX idx_prompt_collection_item_collection_order (collection_id, display_order)
);
