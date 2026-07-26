CREATE INDEX idx_prompt_public_browse
    ON prompt (visibility, deleted_at, created_at, id);
