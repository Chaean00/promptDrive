package com.chaean.promptdrive.prompt.internal.application.ranking;

import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptRankingPeriod;
import com.chaean.promptdrive.prompt.internal.dto.PromptRankingResponse;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.projection.PromptRankingProjection;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptRankingQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final int MAX_OFFSET = 10_000;

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional(readOnly = true)
	public SliceResponse<PromptRankingResponse> getPromptRankings(String periodCode, Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		validatePageRequest(resolvedPage, resolvedSize);
		PromptRankingPeriod period = PromptRankingPeriod.fromCode(periodCode)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));

		Slice<PromptRankingProjection> rankings = promptRepository.findPublicPromptRankings(
			period.resolveLikeCreatedAtLowerBound(java.time.LocalDateTime.now()), PageRequest.of(resolvedPage, resolvedSize));
		if (rankings.isEmpty()) {
			return SliceResponse.from(responseMapper.toPromptRankingResponseSlice(rankings, List.of()));
		}

		var promptIds = rankings.getContent().stream().map(ranking -> ranking.getPrompt().getId()).toList();
		var categories = promptCategoryRepository.findAllByPromptIdIn(promptIds);
		return SliceResponse.from(responseMapper.toPromptRankingResponseSlice(rankings, categories));
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_SIZE || page > MAX_OFFSET / size) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
