package com.chaean.promptdrive.prompt.internal.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptDetailResponse {

	private final Long id;
	private final String title;
	private final String content;
	private final EnumDisplayResponse provenance;
	private final List<EnumDisplayResponse> categories;
	private final String sourceName;
	private final String sourceUrl;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public static PromptDetailResponse from(Prompt prompt, List<PromptCategory> categories) {
		return new PromptDetailResponse(
			prompt.getId(), prompt.getTitle(), prompt.getContent(), EnumDisplayResponse.from(prompt.getProvenance()),
			categories.stream().map(PromptCategory::getCategory).map(EnumDisplayResponse::from).toList(),
			prompt.getSourceName(), prompt.getSourceUrl(), prompt.getCreatedAt(), prompt.getUpdatedAt()
		);
	}
}
