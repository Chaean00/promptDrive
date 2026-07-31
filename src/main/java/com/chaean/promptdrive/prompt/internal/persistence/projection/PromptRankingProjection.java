package com.chaean.promptdrive.prompt.internal.persistence.projection;

import com.chaean.promptdrive.prompt.internal.persistence.Prompt;

public interface PromptRankingProjection {

	Prompt getPrompt();

	Long getLikeCount();
}
