package com.chaean.promptdrive.prompt.internal.application.community;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.dto.CreateCommunityPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.UpdateCommunityPromptRequest;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityPromptCommandService {

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional
	public PromptDetailResponse createCommunityPrompt(Long ownerMemberId, CreateCommunityPromptRequest request) {
		Set<PromptCategoryType> categories = validatePromptCategories(request.getCategories());

		Prompt prompt = promptRepository.save(Prompt.createCommunityPrompt(ownerMemberId, request.getTitle(), request.getContent()));

		replacePromptCategories(prompt, categories, List.of());

		return responseMapper.toPromptDetailResponse(prompt, promptCategoryRepository.findAllByPromptId(prompt.getId()));
	}

	@Transactional
	public PromptDetailResponse updateCommunityPrompt(Long ownerMemberId, Long promptId, UpdateCommunityPromptRequest request) {
		Prompt prompt = promptRepository.findByIdAndOwnerMemberIdAndProvenanceForUpdate(promptId, ownerMemberId, PromptProvenance.COMMUNITY)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

		List<PromptCategory> existing = promptCategoryRepository.findAllByPromptId(promptId);

		Set<PromptCategoryType> categories = validatePromptCategories(request.getCategories());
		prompt.updateCommunityPrompt(ownerMemberId, request.getTitle(), request.getContent());
		replacePromptCategories(prompt, categories, existing);

		return responseMapper.toPromptDetailResponse(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public void deleteCommunityPrompt(Long ownerMemberId, Long promptId) {
		Prompt prompt = promptRepository.findByIdAndOwnerMemberIdAndProvenanceForUpdate(promptId, ownerMemberId, PromptProvenance.COMMUNITY)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

		promptCategoryRepository.findAllByPromptId(promptId).forEach(PromptCategory::softDelete);
		prompt.softDeleteCommunityPrompt(ownerMemberId);
	}

	private void replacePromptCategories(Prompt prompt, Set<PromptCategoryType> desired, List<PromptCategory> existing) {
		Set<PromptCategoryType> current = existing.stream()
				.map(PromptCategory::getCategory)
				.collect(Collectors.toSet());

		existing.stream().filter(category -> !desired.contains(category.getCategory()))
				.forEach(PromptCategory::softDelete);

		List<PromptCategory> additions = desired.stream().filter(category -> !current.contains(category))
			.map(category -> PromptCategory.createPromptCategory(prompt, category)).toList();
		if (!additions.isEmpty()) {
			promptCategoryRepository.saveAll(additions);
		}
	}

	private Set<PromptCategoryType> validatePromptCategories(List<PromptCategoryType> categories) {
		if (categories == null || categories.stream().anyMatch(Objects::isNull)
			|| new HashSet<>(categories).size() != categories.size()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return categories.isEmpty() ? EnumSet.noneOf(PromptCategoryType.class) : EnumSet.copyOf(categories);
	}

}
