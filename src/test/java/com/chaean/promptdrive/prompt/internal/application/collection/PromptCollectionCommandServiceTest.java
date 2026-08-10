package com.chaean.promptdrive.prompt.internal.application.collection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.dto.CreatePromptCollectionRequest;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionItemRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptCollectionCommandServiceTest {

	@Mock
	private PromptCollectionRepository collectionRepository;

	@Mock
	private PromptCollectionItemRepository itemRepository;

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptCategoryRepository categoryRepository;

	@Test
	void rejectsDuplicatePromptIdsBeforeSaving() {
		PromptCollectionCommandService service = new PromptCollectionCommandService(
			collectionRepository, itemRepository, promptRepository, categoryRepository, new PromptResponseMapper());

		CreatePromptCollectionRequest request = new CreatePromptCollectionRequest(
			"developers", "Developers", "Useful prompts", List.of(1L, 1L));

		assertThatThrownBy(() -> service.create(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
		verify(collectionRepository, never()).save(any());
	}

	@Test
	void rejectsNonPublicOrMissingPrompts() {
		PromptCollectionCommandService service = new PromptCollectionCommandService(
			collectionRepository, itemRepository, promptRepository, categoryRepository, new PromptResponseMapper());
		when(promptRepository.findByIdAndVisibility(1L,
			com.chaean.promptdrive.prompt.internal.domain.PromptVisibility.PUBLIC)).thenReturn(Optional.empty());

		CreatePromptCollectionRequest request = new CreatePromptCollectionRequest(
			"developers", "Developers", "Useful prompts", List.of(1L));

		assertThatThrownBy(() -> service.create(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> org.assertj.core.api.Assertions.assertThat(((BusinessException) error).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
		verify(collectionRepository, never()).save(any());
	}
}
