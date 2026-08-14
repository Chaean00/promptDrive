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

	private static final int PREVIEW_LENGTH = 160;

	private final Long id;
	private final String title;
	private final String preview;
	private final EnumDisplayResponse provenance;
	private final List<EnumDisplayResponse> categories;
	private final LocalDateTime createdAt;

	public static PromptSummaryResponse from(Prompt prompt, List<PromptCategory> categories) {
		return new PromptSummaryResponse(
			prompt.getId(), prompt.getTitle(), previewOf(prompt.getContent()), EnumDisplayResponse.from(prompt.getProvenance()),
			categories.stream().map(PromptCategory::getCategory).map(EnumDisplayResponse::from).toList(),
			prompt.getCreatedAt()
		);
	}

	public static String previewOf(String content) {
		String normalized = content.replaceAll("\\s+", " ").trim();
		return normalized.codePointCount(0, normalized.length()) <= PREVIEW_LENGTH
			? normalized
			: normalized.substring(0, normalized.offsetByCodePoints(0, PREVIEW_LENGTH));
	}
}
