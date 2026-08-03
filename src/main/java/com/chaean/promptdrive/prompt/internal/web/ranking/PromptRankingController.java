package com.chaean.promptdrive.prompt.internal.web.ranking;

import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.ranking.PromptRankingQueryService;
import com.chaean.promptdrive.prompt.internal.dto.PromptRankingResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prompts/rankings")
@RequiredArgsConstructor
public class PromptRankingController {

	private final PromptRankingQueryService promptRankingQueryService;

	@GetMapping
	public SliceResponse<PromptRankingResponse> getPromptRankings(
		@RequestParam(defaultValue = "all") String period,
		@RequestParam(defaultValue = "0") Integer page,
		@RequestParam(defaultValue = "20") Integer size
	) {
		return promptRankingQueryService.getPromptRankings(period, page, size);
	}
}
