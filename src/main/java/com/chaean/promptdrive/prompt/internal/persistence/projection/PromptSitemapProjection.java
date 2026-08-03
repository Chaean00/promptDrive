package com.chaean.promptdrive.prompt.internal.persistence.projection;

import java.time.LocalDateTime;

public interface PromptSitemapProjection {

	Long getId();

	LocalDateTime getUpdatedAt();
}
