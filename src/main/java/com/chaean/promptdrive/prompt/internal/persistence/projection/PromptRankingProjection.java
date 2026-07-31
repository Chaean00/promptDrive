package com.chaean.promptdrive.prompt.internal.persistence;

public interface PromptRankingProjection {

	Prompt getPrompt();

	Long getLikeCount();
}
