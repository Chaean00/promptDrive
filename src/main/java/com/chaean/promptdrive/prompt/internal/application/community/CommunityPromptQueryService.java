package com.chaean.promptdrive.prompt.internal.application.community;

import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
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
public class CommunityPromptQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final int MAX_OFFSET = 10_000;

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional(readOnly = true)
	public SliceResponse<PromptSummaryResponse> getOwnedCommunityPromptPage(Long ownerMemberId, Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		validatePageRequest(resolvedPage, resolvedSize);

		var prompts = promptRepository.findAllByOwnerMemberIdAndProvenanceOrderByCreatedAtDescIdDesc(ownerMemberId,
			PromptProvenance.COMMUNITY, PageRequest.of(resolvedPage, resolvedSize));
		var categories = prompts.getContent().isEmpty()
			? List.<PromptCategory>of()
			: promptCategoryRepository.findAllByPromptIdIn(prompts.getContent().stream().map(Prompt::getId).toList());
		return SliceResponse.from(responseMapper.toPromptSummaryResponseSlice(prompts, categories));
	}

	@Transactional(readOnly = true)
	public PromptDetailResponse getOwnedCommunityPrompt(Long ownerMemberId, Long promptId) {
		Prompt prompt = promptRepository.findByIdAndOwnerMemberIdAndProvenance(promptId, ownerMemberId, PromptProvenance.COMMUNITY)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		return responseMapper.toPromptDetailResponse(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_SIZE || page > MAX_OFFSET / size) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
