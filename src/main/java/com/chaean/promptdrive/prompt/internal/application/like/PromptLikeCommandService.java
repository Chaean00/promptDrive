package com.chaean.promptdrive.prompt.internal.application.like;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.PromptLikeResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptLike;
import com.chaean.promptdrive.prompt.internal.persistence.PromptLikeRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptLikeCommandService {

	private final PromptRepository promptRepository;
	private final PromptLikeRepository promptLikeRepository;

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public PromptLikeResponse registerPromptLike(Long memberId, Long promptId) {
		promptRepository.findByIdAndVisibilityForUpdate(promptId, PromptVisibility.PUBLIC)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

		if (promptLikeRepository.reactivateLatestDeletedByPromptIdAndMemberId(promptId, memberId) == 0) {
			promptLikeRepository.insertIfAbsent(promptId, memberId);
		}

		return PromptLikeResponse.of(promptId, true);
	}

	@Transactional
	public PromptLikeResponse removePromptLike(Long memberId, Long promptId) {
		promptRepository.findByIdAndVisibilityForUpdate(promptId, PromptVisibility.PUBLIC)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

		promptLikeRepository.findByPromptIdAndMemberId(promptId, memberId).ifPresent(PromptLike::softDelete);
		return PromptLikeResponse.of(promptId, false);
	}
}
