package com.chaean.promptdrive.prompt.internal.application.catalog;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.CuratedPromptResponse;
import com.chaean.promptdrive.prompt.internal.dto.UpdateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuratedPromptCommandService {

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional
	public CuratedPromptResponse createCuratedPrompt(CreateCuratedPromptRequest request) {
		Set<PromptCategoryType> categories = validatePromptCategories(request.getCategories());
		Prompt prompt = promptRepository.save(Prompt.createCuratedPrompt(request.getTitle(), request.getContent(),
			request.getVisibility(), request.getSourceName(), request.getSourceUrl()));
		replacePromptCategories(prompt, categories, List.of());
		return responseMapper.toCuratedPromptResponse(prompt, promptCategoryRepository.findAllByPromptId(prompt.getId()));
	}

	@Transactional
	public CuratedPromptResponse updateCuratedPrompt(Long promptId, UpdateCuratedPromptRequest request) {
		Prompt prompt = findCuratedPromptForUpdate(promptId);
		List<PromptCategory> existing = promptCategoryRepository.findAllByPromptId(promptId);
		Set<PromptCategoryType> categories = validatePromptCategories(request.getCategories());
		prompt.updateCuratedPrompt(request.getTitle(), request.getContent(), request.getSourceName(), request.getSourceUrl());
		replacePromptCategories(prompt, categories, existing);
		return responseMapper.toCuratedPromptResponse(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public CuratedPromptResponse changeCuratedPromptVisibility(Long promptId, PromptVisibility visibility) {
		Prompt prompt = findCuratedPromptForUpdate(promptId);
		prompt.changeCuratedPromptVisibility(visibility);
		return responseMapper.toCuratedPromptResponse(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public void deleteCuratedPrompt(Long promptId) {
		Prompt prompt = findCuratedPromptForUpdate(promptId);
		promptCategoryRepository.findAllByPromptId(promptId).forEach(PromptCategory::softDelete);
		prompt.softDeleteCuratedPrompt();
	}

	private Prompt findCuratedPromptForUpdate(Long promptId) {
		return promptRepository.findByIdAndProvenanceForUpdate(promptId, PromptProvenance.CURATED)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	private void replacePromptCategories(Prompt prompt, Set<PromptCategoryType> desired, List<PromptCategory> existing) {
		Set<PromptCategoryType> current = existing.stream().map(PromptCategory::getCategory).collect(Collectors.toSet());
		existing.stream().filter(category -> !desired.contains(category.getCategory())).forEach(PromptCategory::softDelete);
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
