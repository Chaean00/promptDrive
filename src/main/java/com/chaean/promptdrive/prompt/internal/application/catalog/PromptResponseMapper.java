package com.chaean.promptdrive.prompt.internal.application.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.chaean.promptdrive.prompt.internal.dto.CuratedPromptResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;

import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
public class PromptResponseMapper {

	private static final Comparator<PromptCategory> CATEGORY_ORDER = Comparator.comparingInt(
		category -> category.getCategory().ordinal());

	public PromptDetailResponse toDetail(Prompt prompt, List<PromptCategory> categories) {
		return PromptDetailResponse.from(prompt, ordered(categories));
	}

	public CuratedPromptResponse toCurated(Prompt prompt, List<PromptCategory> categories) {
		return CuratedPromptResponse.from(prompt, ordered(categories));
	}

	public Slice<PromptSummaryResponse> toSummarySlice(Slice<Prompt> prompts, List<PromptCategory> categories) {
		Map<Long, List<PromptCategory>> categoriesByPrompt = categories.stream()
			.collect(Collectors.groupingBy(category -> category.getPrompt().getId()));
		return prompts.map(prompt -> PromptSummaryResponse.from(prompt,
			ordered(categoriesByPrompt.getOrDefault(prompt.getId(), List.of()))));
	}

	public Slice<CuratedPromptResponse> toCuratedSlice(Slice<Prompt> prompts, List<PromptCategory> categories) {
		Map<Long, List<PromptCategory>> categoriesByPrompt = categories.stream()
			.collect(Collectors.groupingBy(category -> category.getPrompt().getId()));
		return prompts.map(prompt -> CuratedPromptResponse.from(prompt,
			ordered(categoriesByPrompt.getOrDefault(prompt.getId(), List.of()))));
	}

	private List<PromptCategory> ordered(List<PromptCategory> categories) {
		return categories.stream().sorted(CATEGORY_ORDER).toList();
	}
}
