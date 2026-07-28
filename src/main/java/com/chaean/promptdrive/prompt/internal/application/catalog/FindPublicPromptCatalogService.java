package com.chaean.promptdrive.prompt.internal.application.catalog;

import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindPublicPromptCatalogService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	public SliceResponse<PromptSummaryResponse> findPublicPromptSummaries(PromptProvenance provenance, PromptCategoryType category,
		Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		validatePageRequest(resolvedPage, resolvedSize);
		Slice<Prompt> prompts = promptRepository.findPublicPrompts(provenance, category,
			PageRequest.of(resolvedPage, resolvedSize));
		if (prompts.isEmpty()) {
			return SliceResponse.from(responseMapper.toPromptSummaryResponseSlice(prompts, List.of()));
		}
		var ids = prompts.getContent().stream().map(Prompt::getId).toList();
		var categories = promptCategoryRepository.findAllByPromptIdIn(ids);
		return SliceResponse.from(responseMapper.toPromptSummaryResponseSlice(prompts, categories));
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_SIZE) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
