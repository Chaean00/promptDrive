package com.chaean.promptdrive.prompt.internal.application.community;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.dto.CreateCommunityPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
import com.chaean.promptdrive.prompt.internal.dto.UpdateCommunityPromptRequest;
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
public class MaintainCommunityPromptService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	public SliceResponse<PromptSummaryResponse> browse(Long ownerMemberId, Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		validatePage(resolvedPage, resolvedSize);
		var prompts = promptRepository.findAllByOwnerMemberIdAndProvenanceOrderByCreatedAtDescIdDesc(ownerMemberId,
			PromptProvenance.COMMUNITY, PageRequest.of(resolvedPage, resolvedSize));
		return SliceResponse.from(responseMapper.toSummarySlice(prompts,
			categoriesFor(prompts.getContent().stream().map(Prompt::getId).toList())));
	}

	public PromptDetailResponse get(Long ownerMemberId, Long promptId) {
		Prompt prompt = promptRepository.findByIdAndOwnerMemberIdAndProvenance(promptId, ownerMemberId, PromptProvenance.COMMUNITY)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

		return responseMapper.toDetail(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public PromptDetailResponse create(Long ownerMemberId, CreateCommunityPromptRequest request) {
		Set<PromptCategoryType> categories = validateCategories(request.getCategories());
		Prompt prompt = promptRepository.save(Prompt.createCommunity(ownerMemberId, request.getTitle(), request.getContent()));
		replaceCategories(prompt, categories, List.of());
		return responseMapper.toDetail(prompt, promptCategoryRepository.findAllByPromptId(prompt.getId()));
	}

	@Transactional
	public PromptDetailResponse update(Long ownerMemberId, Long promptId, UpdateCommunityPromptRequest request) {
		Prompt prompt = promptRepository.findByIdAndOwnerMemberIdAndProvenance(promptId, ownerMemberId, PromptProvenance.COMMUNITY)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

		List<PromptCategory> existing = promptCategoryRepository.findAllByPromptId(promptId);

		Set<PromptCategoryType> categories = validateCategories(request.getCategories());
		prompt.updateCommunity(ownerMemberId, request.getTitle(), request.getContent());
		replaceCategories(prompt, categories, existing);
		return responseMapper.toDetail(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public void delete(Long ownerMemberId, Long promptId) {
		Prompt prompt = promptRepository.findByIdAndOwnerMemberIdAndProvenance(promptId, ownerMemberId, PromptProvenance.COMMUNITY)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

		promptCategoryRepository.findAllByPromptId(promptId).forEach(PromptCategory::delete);
		prompt.deleteCommunity(ownerMemberId);
	}

	private void replaceCategories(Prompt prompt, Set<PromptCategoryType> desired, List<PromptCategory> existing) {
		Set<PromptCategoryType> current = existing.stream()
				.map(PromptCategory::getCategory)
				.collect(Collectors.toSet());

		existing.stream().filter(category -> !desired.contains(category.getCategory()))
				.forEach(PromptCategory::delete);

		List<PromptCategory> additions = desired.stream().filter(category -> !current.contains(category))
			.map(category -> PromptCategory.create(prompt, category)).toList();
		if (!additions.isEmpty()) {
			promptCategoryRepository.saveAll(additions);
		}
	}

	private List<PromptCategory> categoriesFor(List<Long> promptIds) {
		return promptIds.isEmpty() ? List.of() : promptCategoryRepository.findAllByPromptIdIn(promptIds);
	}

	private Set<PromptCategoryType> validateCategories(List<PromptCategoryType> categories) {
		if (categories == null || categories.stream().anyMatch(Objects::isNull)
			|| new HashSet<>(categories).size() != categories.size()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return categories.isEmpty() ? EnumSet.noneOf(PromptCategoryType.class) : EnumSet.copyOf(categories);
	}

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_SIZE) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
