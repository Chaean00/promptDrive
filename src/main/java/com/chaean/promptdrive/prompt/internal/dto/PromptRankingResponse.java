package com.chaean.promptdrive.prompt.internal.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PromptRankingResponse {

	private final Long id;
	private final String title;
	private final EnumDisplayResponse provenance;
	private final List<EnumDisplayResponse> categories;
	private final LocalDateTime createdAt;
	private final Long likeCount;

	public static PromptRankingResponse from(Prompt prompt, List<PromptCategory> categories, Long likeCount) {
		return new PromptRankingResponse(
			prompt.getId(), prompt.getTitle(), EnumDisplayResponse.from(prompt.getProvenance()),
			categories.stream().map(PromptCategory::getCategory).map(EnumDisplayResponse::from).toList(),
			prompt.getCreatedAt(), likeCount
		);
	}
}
