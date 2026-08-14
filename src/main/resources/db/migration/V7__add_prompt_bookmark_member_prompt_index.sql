CREATE INDEX idx_prompt_bookmark_member_id_prompt_id
    ON prompt_bookmark (member_id, prompt_id);
