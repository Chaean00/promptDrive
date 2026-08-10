package com.chaean.promptdrive.prompt.internal.application.collection;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreatePromptCollectionRequest;
import com.chaean.promptdrive.prompt.internal.dto.PromptCollectionResponse;
import com.chaean.promptdrive.prompt.internal.dto.UpdatePromptCollectionRequest;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollection;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionItem;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionItemRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptCollectionCommandService {

	private final PromptCollectionRepository collectionRepository;
	private final PromptCollectionItemRepository itemRepository;
	private final PromptRepository promptRepository;
	private final PromptCategoryRepository categoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional
	public PromptCollectionResponse create(CreatePromptCollectionRequest request) {
		List<Prompt> prompts = validateAndLoadPrompts(request.getPromptIds());
		PromptCollection collection = collectionRepository.save(PromptCollection.create(
			request.getSlug().trim(), request.getTitle().trim(), request.getDescription().trim()));
		saveItems(collection, prompts);
		return toResponse(collection, prompts);
	}

	@Transactional
	public PromptCollectionResponse update(Long collectionId, UpdatePromptCollectionRequest request) {
		PromptCollection collection = findCollection(collectionId);
		List<Prompt> prompts = validateAndLoadPrompts(request.getPromptIds());
		itemRepository.findAllByCollectionId(collectionId).forEach(PromptCollectionItem::softDelete);
		collection.update(request.getSlug().trim(), request.getTitle().trim(), request.getDescription().trim());
		saveItems(collection, prompts);
		return toResponse(collection, prompts);
	}

	@Transactional
	public void delete(Long collectionId) {
		PromptCollection collection = findCollection(collectionId);
		itemRepository.findAllByCollectionId(collectionId).forEach(PromptCollectionItem::softDelete);
		collection.softDelete();
	}

	private PromptCollection findCollection(Long id) {
		return collectionRepository.findById(id)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	private List<Prompt> validateAndLoadPrompts(List<Long> promptIds) {
		if (promptIds == null || promptIds.stream().anyMatch(Objects::isNull)
			|| new HashSet<>(promptIds).size() != promptIds.size()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		List<Prompt> prompts = promptIds.stream().map(id -> promptRepository.findByIdAndVisibility(id, PromptVisibility.PUBLIC)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND))).toList();
		return prompts;
	}

	private void saveItems(PromptCollection collection, List<Prompt> prompts) {
		itemRepository.saveAll(java.util.stream.IntStream.range(0, prompts.size())
			.mapToObj(index -> PromptCollectionItem.create(collection, prompts.get(index), index)).toList());
	}

	private PromptCollectionResponse toResponse(PromptCollection collection, List<Prompt> prompts) {
		List<PromptCategory> categories = prompts.isEmpty() ? List.of()
			: categoryRepository.findAllByPromptIdIn(prompts.stream().map(Prompt::getId).toList());
		return PromptCollectionResponse.from(collection, responseMapper.toPromptSummaryResponsePage(
			new org.springframework.data.domain.PageImpl<>(prompts), categories).getContent());
	}

}
