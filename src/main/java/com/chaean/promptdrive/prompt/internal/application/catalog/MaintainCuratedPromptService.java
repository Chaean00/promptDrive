package com.chaean.promptdrive.prompt.internal.application.catalog;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.response.SliceResponse;
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

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaintainCuratedPromptService {

	private final PromptRepository promptRepository;
	private final PromptCategoryRepository promptCategoryRepository;
	private final PromptResponseMapper responseMapper;

	public SliceResponse<CuratedPromptResponse> browse(PromptVisibility visibility, Integer page, Integer size) {
		int resolvedPage = page == null ? 0 : page;
		int resolvedSize = size == null ? 20 : size;
		validatePage(resolvedPage, resolvedSize);
		var prompts = promptRepository.findCuratedPrompts(visibility,
			PageRequest.of(resolvedPage, resolvedSize));
		var categories = categoriesFor(prompts.getContent().stream().map(Prompt::getId).toList());
		return SliceResponse.from(responseMapper.toCuratedSlice(prompts, categories));
	}

	public CuratedPromptResponse get(Long promptId) {
		Prompt prompt = findCurated(promptId);
		return responseMapper.toCurated(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public CuratedPromptResponse create(CreateCuratedPromptRequest request) {
		Set<PromptCategoryType> categories = validateCategories(request.getCategories());
		Prompt prompt = promptRepository.save(Prompt.createCurated(request.getTitle(), request.getContent(),
			request.getVisibility(), request.getSourceName(), request.getSourceUrl()));
		replaceCategories(prompt, categories, List.of());
		return responseMapper.toCurated(prompt, promptCategoryRepository.findAllByPromptId(prompt.getId()));
	}

	@Transactional
	public CuratedPromptResponse update(Long promptId, UpdateCuratedPromptRequest request) {
		Prompt prompt = findCurated(promptId);
		List<PromptCategory> existing = promptCategoryRepository.findAllByPromptId(promptId);
		Set<PromptCategoryType> categories = validateCategories(request.getCategories());
		prompt.updateCurated(request.getTitle(), request.getContent(), request.getSourceName(), request.getSourceUrl());
		replaceCategories(prompt, categories, existing);
		return responseMapper.toCurated(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public CuratedPromptResponse changeVisibility(Long promptId, PromptVisibility visibility) {
		Prompt prompt = findCurated(promptId);
		prompt.changeVisibility(visibility);
		return responseMapper.toCurated(prompt, promptCategoryRepository.findAllByPromptId(promptId));
	}

	@Transactional
	public void delete(Long promptId) {
		Prompt prompt = findCurated(promptId);
		promptCategoryRepository.findAllByPromptId(promptId).forEach(PromptCategory::delete);
		prompt.deleteCurated();
	}

	private Prompt findCurated(Long promptId) {
		return promptRepository.findByIdAndProvenance(promptId, PromptProvenance.CURATED)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	private void replaceCategories(Prompt prompt, Set<PromptCategoryType> desired, List<PromptCategory> existing) {
		Set<PromptCategoryType> current = existing.stream().map(PromptCategory::getCategory).collect(Collectors.toSet());
		existing.stream().filter(category -> !desired.contains(category.getCategory())).forEach(PromptCategory::delete);
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
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}
}
