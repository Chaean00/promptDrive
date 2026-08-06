package com.chaean.promptdrive.prompt.internal.application.catalog;

import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.PageResponse;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicPromptQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final int MAX_OFFSET = 10_000;

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional(readOnly = true)
	public PageResponse<PromptSummaryResponse> getPublicPromptPage(
			PromptProvenance provenance,
			PromptCategoryType category,
			Integer page,
			Integer size
	) {
		return getPublicPromptPage(provenance, category, null, page, size);
	}

	public PageResponse<PromptSummaryResponse> getPublicPromptPage(
			PromptProvenance provenance,
			PromptCategoryType category,
			String keyword,
			Integer page,
			Integer size
	) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		validatePageRequest(resolvedPage, resolvedSize);
		String normalizedKeyword = normalizeKeyword(keyword);
		Page<Prompt> prompts = promptRepository.findPublicPrompts(provenance, category, normalizedKeyword,
			PageRequest.of(resolvedPage, resolvedSize));
		var categories = prompts.isEmpty()
			? List.<PromptCategory>of()
			: promptCategoryRepository.findAllByPromptIdIn(prompts.getContent().stream().map(Prompt::getId).toList());
		return PageResponse.from(responseMapper.toPromptSummaryResponsePage(prompts, categories));
	}

	public PromptDetailResponse getPublicPromptDetail(Long promptId) {
		Prompt prompt = promptRepository.findByIdAndVisibility(promptId, PromptVisibility.PUBLIC)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		return responseMapper.toPromptDetailResponse(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_SIZE || page > MAX_OFFSET / size) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		String normalizedKeyword = keyword.trim();
		return normalizedKeyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
