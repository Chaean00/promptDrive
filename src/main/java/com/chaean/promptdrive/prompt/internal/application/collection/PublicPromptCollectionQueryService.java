package com.chaean.promptdrive.prompt.internal.application.collection;

import java.util.List;
import java.util.Map;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.dto.PromptCollectionResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptCollectionSummaryResponse;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionItem;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionItemRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicPromptCollectionQueryService {

	private final PromptCollectionRepository collectionRepository;
	private final PromptCollectionItemRepository itemRepository;
	private final PromptCategoryRepository categoryRepository;
	private final PromptResponseMapper responseMapper;

	@Transactional(readOnly = true)
	public PromptCollectionResponse getBySlug(String slug) {
		var collection = collectionRepository.findBySlug(slug)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		List<PromptCollectionItem> items = itemRepository.findPublicItemsByCollectionId(collection.getId());
		var prompts = items.stream().map(PromptCollectionItem::getPrompt).toList();
		List<PromptCategory> categories = prompts.isEmpty() ? List.of()
			: categoryRepository.findAllByPromptIdIn(prompts.stream().map(prompt -> prompt.getId()).toList());
		return PromptCollectionResponse.from(collection, responseMapper.toPromptSummaryResponsePage(
			new org.springframework.data.domain.PageImpl<>(prompts), categories).getContent());
	}

	@Transactional(readOnly = true)
	public List<PromptCollectionSummaryResponse> getPublicCollections() {
		Map<Long, Long> promptCounts = itemRepository.countPublicPromptsByCollection().stream()
			.collect(java.util.stream.Collectors.toMap(
				item -> item.getCollectionId(), item -> item.getPromptCount()));
		return collectionRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
			.map(collection -> PromptCollectionSummaryResponse.from(collection,
				promptCounts.getOrDefault(collection.getId(), 0L)))
			.toList();
	}
}
