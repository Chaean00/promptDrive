package com.chaean.promptdrive.prompt.internal.application.copy;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.PromptCopyResponse;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptCopyCommandService {

	private final PromptRepository promptRepository;

	@Transactional
	public PromptCopyResponse registerPromptCopy(Long promptId) {
		if (promptRepository.incrementPublicCopyCount(promptId) == 0) {
			throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
		}
		long copyCount = promptRepository.findByIdAndVisibility(promptId, PromptVisibility.PUBLIC)
			.map(prompt -> prompt.getCopyCount())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		return PromptCopyResponse.of(promptId, copyCount);
	}
}
