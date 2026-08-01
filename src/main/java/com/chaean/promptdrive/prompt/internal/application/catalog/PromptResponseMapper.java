package com.chaean.promptdrive.prompt.internal.application.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.chaean.promptdrive.prompt.internal.dto.CuratedPromptResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptRankingResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.projection.PromptRankingProjection;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PromptResponseMapper {

	private static final Comparator<PromptCategory> CATEGORY_ORDER = Comparator.comparingInt(
		category -> category.getCategory().ordinal());

	public PromptDetailResponse toPromptDetailResponse(Prompt prompt, List<PromptCategory> categories) {
		return PromptDetailResponse.from(prompt, orderCategoriesByType(categories));
	}

	public CuratedPromptResponse toCuratedPromptResponse(Prompt prompt, List<PromptCategory> categories) {
		return CuratedPromptResponse.from(prompt, orderCategoriesByType(categories));
	}

	public Slice<PromptSummaryResponse> toPromptSummaryResponseSlice(Slice<Prompt> prompts, List<PromptCategory> categories) {
		Map<Long, List<PromptCategory>> categoriesByPrompt = categories.stream()
			.collect(Collectors.groupingBy(category -> category.getPrompt().getId()));
		return prompts.map(prompt -> PromptSummaryResponse.from(prompt,
			orderCategoriesByType(categoriesByPrompt.getOrDefault(prompt.getId(), List.of()))));
	}

	public Page<PromptSummaryResponse> toPromptSummaryResponsePage(Page<Prompt> prompts, List<PromptCategory> categories) {
		Map<Long, List<PromptCategory>> categoriesByPrompt = categories.stream()
			.collect(Collectors.groupingBy(category -> category.getPrompt().getId()));
		return prompts.map(prompt -> PromptSummaryResponse.from(prompt,
			orderCategoriesByType(categoriesByPrompt.getOrDefault(prompt.getId(), List.of()))));
	}

	public Slice<CuratedPromptResponse> toCuratedPromptResponseSlice(Slice<Prompt> prompts, List<PromptCategory> categories) {
		Map<Long, List<PromptCategory>> categoriesByPrompt = categories.stream()
			.collect(Collectors.groupingBy(category -> category.getPrompt().getId()));
		return prompts.map(prompt -> CuratedPromptResponse.from(prompt,
			orderCategoriesByType(categoriesByPrompt.getOrDefault(prompt.getId(), List.of()))));
	}

	public Slice<PromptRankingResponse> toPromptRankingResponseSlice(Slice<PromptRankingProjection> rankings,
		List<PromptCategory> categories) {
		Map<Long, List<PromptCategory>> categoriesByPrompt = categories.stream()
			.collect(Collectors.groupingBy(category -> category.getPrompt().getId()));
		return rankings.map(ranking -> PromptRankingResponse.from(ranking.getPrompt(),
			orderCategoriesByType(categoriesByPrompt.getOrDefault(ranking.getPrompt().getId(), List.of())), ranking.getLikeCount()));
	}

	private List<PromptCategory> orderCategoriesByType(List<PromptCategory> categories) {
		return categories.stream().sorted(CATEGORY_ORDER).toList();
	}
}
