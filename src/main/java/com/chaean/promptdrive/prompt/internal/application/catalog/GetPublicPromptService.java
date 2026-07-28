package com.chaean.promptdrive.prompt.internal.application.catalog;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPublicPromptService {

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	public PromptDetailResponse findPublicPrompt(Long promptId) {
		var prompt = promptRepository.findByIdAndVisibility(promptId, PromptVisibility.PUBLIC)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		return responseMapper.toPromptDetailResponse(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}
}
