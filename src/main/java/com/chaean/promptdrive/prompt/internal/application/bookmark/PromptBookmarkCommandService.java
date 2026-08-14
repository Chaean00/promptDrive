package com.chaean.promptdrive.prompt.internal.application.bookmark;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.PromptBookmarkResponse;
import com.chaean.promptdrive.prompt.internal.persistence.PromptBookmarkRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptBookmarkCommandService {

	private final PromptRepository promptRepository;
	private final PromptBookmarkRepository promptBookmarkRepository;

	@Transactional
	public PromptBookmarkResponse registerPromptBookmark(Long memberId, Long promptId) {
		promptBookmarkRepository.insertPublicPromptBookmarkIfAbsent(promptId, memberId);
		if (!promptRepository.existsByIdAndVisibility(promptId, PromptVisibility.PUBLIC)) {
			throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
		}
		return PromptBookmarkResponse.of(promptId, true);
	}

	@Transactional
	public PromptBookmarkResponse removePromptBookmark(Long memberId, Long promptId) {
		promptBookmarkRepository.softDeleteActiveByPromptIdAndMemberId(promptId, memberId);
		return PromptBookmarkResponse.of(promptId, false);
	}
}
