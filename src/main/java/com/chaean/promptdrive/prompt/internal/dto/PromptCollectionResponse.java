package com.chaean.promptdrive.prompt.internal.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.chaean.promptdrive.prompt.internal.persistence.PromptCollection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PromptCollectionResponse {

	private final Long id;
	private final String slug;
	private final String title;
	private final String description;
	private final List<PromptSummaryResponse> prompts;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public static PromptCollectionResponse from(PromptCollection collection, List<PromptSummaryResponse> prompts) {
		return new PromptCollectionResponse(collection.getId(), collection.getSlug(), collection.getTitle(),
			collection.getDescription(), List.copyOf(prompts), collection.getCreatedAt(), collection.getUpdatedAt());
	}
}
