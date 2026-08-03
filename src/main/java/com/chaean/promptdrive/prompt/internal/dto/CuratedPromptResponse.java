package com.chaean.promptdrive.prompt.internal.dto;

import java.util.List;

import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;

import lombok.Getter;

@Getter
public class CuratedPromptResponse extends PromptDetailResponse {

	private final EnumDisplayResponse visibility;
	private final String sourceName;
	private final String sourceUrl;

	private CuratedPromptResponse(Prompt prompt, List<PromptCategory> categories) {
		super(prompt.getId(), prompt.getTitle(), prompt.getContent(), EnumDisplayResponse.from(prompt.getProvenance()),
			categories.stream().map(PromptCategory::getCategory).map(EnumDisplayResponse::from).toList(),
			prompt.getCreatedAt(), prompt.getUpdatedAt());
		this.visibility = EnumDisplayResponse.from(prompt.getVisibility());
		this.sourceName = prompt.getSourceName();
		this.sourceUrl = prompt.getSourceUrl();
	}

	public static CuratedPromptResponse from(Prompt prompt, List<PromptCategory> categories) {
		return new CuratedPromptResponse(prompt, categories);
	}
}
