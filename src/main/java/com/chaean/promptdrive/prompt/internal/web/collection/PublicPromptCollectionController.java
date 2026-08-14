package com.chaean.promptdrive.prompt.internal.web.collection;

import java.util.List;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.prompt.internal.application.collection.PublicPromptCollectionQueryService;
import com.chaean.promptdrive.prompt.internal.dto.PromptCollectionResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptCollectionSummaryResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prompt-collections")
@RequiredArgsConstructor
public class PublicPromptCollectionController {

	private final PublicPromptCollectionQueryService queryService;

	@GetMapping
	public ApiResponse<List<PromptCollectionSummaryResponse>> list() {
		return ApiResponse.of(queryService.getPublicCollections());
	}

	@GetMapping("/{slug}")
	public ApiResponse<PromptCollectionResponse> get(@PathVariable String slug) {
		return ApiResponse.of(queryService.getBySlug(slug));
	}
}
