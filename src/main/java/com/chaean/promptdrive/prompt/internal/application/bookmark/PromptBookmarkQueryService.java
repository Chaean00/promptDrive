package com.chaean.promptdrive.prompt.internal.application.bookmark;

import java.util.HashSet;
import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptBookmarkRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptBookmarkQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final int MAX_OFFSET = 10_000;

	private final PromptBookmarkRepository promptBookmarkRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional(readOnly = true)
	public SliceResponse<PromptSummaryResponse> getBookmarkedPromptPage(Long memberId, Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		if (resolvedPage < 0 || resolvedSize < 1 || resolvedSize > MAX_SIZE || resolvedPage > MAX_OFFSET / resolvedSize) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}

		var prompts = promptBookmarkRepository.findBookmarkedPublicPrompts(memberId, PageRequest.of(resolvedPage, resolvedSize));
		var categories = prompts.getContent().isEmpty()
			? List.<PromptCategory>of()
			: promptCategoryRepository.findAllByPromptIdIn(prompts.getContent().stream().map(Prompt::getId).toList());
		return SliceResponse.from(responseMapper.toPromptSummaryResponseSlice(prompts, categories));
	}

	@Transactional(readOnly = true)
	public List<Long> getBookmarkedPromptIds(Long memberId) {
		return promptBookmarkRepository.findBookmarkedPublicPromptIds(memberId);
	}

	@Transactional(readOnly = true)
	public List<Long> getBookmarkedPromptIds(Long memberId, List<Long> promptIds) {
		if (promptIds == null || promptIds.size() > 100 || promptIds.stream().anyMatch(id -> id == null || id < 1)
			|| new HashSet<>(promptIds).size() != promptIds.size()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return promptIds.isEmpty() ? List.of() : promptBookmarkRepository.findBookmarkedPublicPromptIdsByMemberIdAndPromptIdIn(memberId, promptIds);
	}
}
