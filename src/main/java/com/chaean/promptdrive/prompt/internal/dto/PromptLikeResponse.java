package com.chaean.promptdrive.prompt.internal.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PromptLikeResponse {

	private final Long promptId;
	private final boolean liked;

	public static PromptLikeResponse of(Long promptId, boolean liked) {
		return new PromptLikeResponse(promptId, liked);
	}
}
