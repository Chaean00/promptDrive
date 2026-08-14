package com.chaean.promptdrive.prompt.internal.dto;

import com.chaean.promptdrive.prompt.internal.persistence.PromptCollection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PromptCollectionSummaryResponse {

	private final Long id;
	private final String slug;
	private final String title;
	private final String description;
	private final long promptCount;

	public static PromptCollectionSummaryResponse from(PromptCollection collection, long promptCount) {
		return new PromptCollectionSummaryResponse(collection.getId(), collection.getSlug(), collection.getTitle(),
			collection.getDescription(), promptCount);
	}
}
