package com.chaean.promptdrive.prompt.internal.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PromptBookmarkResponse {

	private final Long promptId;
	private final boolean bookmarked;

	public static PromptBookmarkResponse of(Long promptId, boolean bookmarked) {
		return new PromptBookmarkResponse(promptId, bookmarked);
	}
}
