package com.chaean.promptdrive.prompt.internal.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PromptCopyResponse {

	private final Long promptId;
	private final long copyCount;

	public static PromptCopyResponse of(Long promptId, long copyCount) {
		return new PromptCopyResponse(promptId, copyCount);
	}
}
