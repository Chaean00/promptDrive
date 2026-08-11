package com.chaean.promptdrive.prompt.internal.application.catalog;

import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CuratedPromptResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuratedPromptQueryService {

	private static final int MAX_OFFSET = 10_000;

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional(readOnly = true)
	public SliceResponse<CuratedPromptResponse> getCuratedPromptPage(PromptVisibility visibility, Integer page, Integer size) {
		int resolvedPage = page == null ? 0 : page;
		int resolvedSize = size == null ? 20 : size;
		validatePageRequest(resolvedPage, resolvedSize);
		var prompts = promptRepository.findCuratedPrompts(visibility,
			PageRequest.of(resolvedPage, resolvedSize));
		var categories = prompts.getContent().isEmpty()
			? List.<PromptCategory>of()
			: promptCategoryRepository.findAllByPromptIdIn(prompts.getContent().stream().map(Prompt::getId).toList());
		return SliceResponse.from(responseMapper.toCuratedPromptResponseSlice(prompts, categories));
	}

	@Transactional(readOnly = true)
	public CuratedPromptResponse getCuratedPrompt(Long promptId) {
		Prompt prompt = promptRepository.findByIdAndProvenance(promptId, PromptProvenance.CURATED)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		return responseMapper.toCuratedPromptResponse(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > 100 || page > MAX_OFFSET / size) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
