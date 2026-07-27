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
public class PromptSummaryResponse {

	private final Long id;
	private final String title;
	private final EnumDisplayResponse provenance;
	private final List<EnumDisplayResponse> categories;
	private final LocalDateTime createdAt;

	public static PromptSummaryResponse from(Prompt prompt, List<PromptCategory> categories) {
		return new PromptSummaryResponse(
			prompt.getId(), prompt.getTitle(), EnumDisplayResponse.from(prompt.getProvenance()),
			categories.stream().map(PromptCategory::getCategory).map(EnumDisplayResponse::from).toList(),
			prompt.getCreatedAt()
		);
	}
}
