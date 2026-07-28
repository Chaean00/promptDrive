package com.chaean.promptdrive.prompt.internal.web.catalog;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.catalog.FindPublicPromptCatalogService;
import com.chaean.promptdrive.prompt.internal.application.catalog.GetPublicPromptService;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PublicPromptController {

	private final FindPublicPromptCatalogService findPublicPromptCatalogService;
	private final GetPublicPromptService getPublicPromptService;

	@GetMapping
	public SliceResponse<PromptSummaryResponse> listPublicPrompts(
		@RequestParam(required = false) PromptProvenance provenance,
		@RequestParam(required = false) PromptCategoryType category,
		@RequestParam(defaultValue = "0") Integer page,
		@RequestParam(defaultValue = "20") Integer size
	) {
		return findPublicPromptCatalogService.findPublicPromptSummaries(provenance, category, page, size);
	}

	@GetMapping("/{promptId}")
	public ApiResponse<PromptDetailResponse> getPublicPrompt(@PathVariable Long promptId) {
		return ApiResponse.of(getPublicPromptService.findPublicPrompt(promptId));
	}

}
