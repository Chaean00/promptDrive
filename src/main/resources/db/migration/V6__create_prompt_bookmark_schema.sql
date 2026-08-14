CREATE TABLE prompt_bookmark (
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
    CONSTRAINT uk_prompt_bookmark_active UNIQUE (prompt_id, active_member_id),
    INDEX idx_prompt_bookmark_member_id_updated_at_id (member_id, updated_at, id)
);
