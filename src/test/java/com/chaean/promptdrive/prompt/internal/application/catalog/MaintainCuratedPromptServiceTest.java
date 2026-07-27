package com.chaean.promptdrive.prompt.internal.application.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.UpdateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintainCuratedPromptServiceTest {

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptCategoryRepository promptCategoryRepository;

	private MaintainCuratedPromptService service;

	@BeforeEach
	void setUp() {
		service = new MaintainCuratedPromptService(promptRepository, promptCategoryRepository,
			new PromptResponseMapper());
	}

	@Test
	void rejectsDuplicateCategoriesAsBusinessError() {
		CreateCuratedPromptRequest request = new CreateCuratedPromptRequest(
			"title", "content", List.of(PromptCategoryType.DEVELOPMENT, PromptCategoryType.DEVELOPMENT),
			PromptVisibility.PUBLIC, null, null);

		assertThatThrownBy(() -> service.create(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
		verify(promptRepository, never()).save(any());
	}

	@Test
	void rejectsNullCategoryAsBusinessError() {
		CreateCuratedPromptRequest request = new CreateCuratedPromptRequest(
			"title", "content", java.util.Arrays.asList(PromptCategoryType.DEVELOPMENT, null),
			PromptVisibility.PUBLIC, null, null);

		assertThatThrownBy(() -> service.create(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
	}

	@Test
	void deletesCuratedPromptAndAllActiveCategories() {
		Prompt prompt = Prompt.createCurated("title", "content", PromptVisibility.PUBLIC, null, null);
		PromptCategory category = PromptCategory.create(prompt, PromptCategoryType.DEVELOPMENT);
		when(promptRepository.findByIdAndProvenance(1L, PromptProvenance.CURATED)).thenReturn(Optional.of(prompt));
		when(promptCategoryRepository.findAllByPromptId(1L)).thenReturn(List.of(category));

		service.delete(1L);

		assertThat(prompt.getDeletedAt()).isNotNull();
		assertThat(category.getDeletedAt()).isNotNull();
	}

	@Test
	void updatesCuratedPromptByPreservingExistingAndAddingOnlyNewCategories() {
		Prompt prompt = Prompt.createCurated("title", "content", PromptVisibility.PUBLIC, null, null);
		PromptCategory existing = PromptCategory.create(prompt, PromptCategoryType.DEVELOPMENT);
		PromptCategory removed = PromptCategory.create(prompt, PromptCategoryType.CODE_REVIEW);
		when(promptRepository.findByIdAndProvenance(1L, PromptProvenance.CURATED)).thenReturn(Optional.of(prompt));
		when(promptCategoryRepository.findAllByPromptId(1L)).thenReturn(List.of(existing, removed));

		service.update(1L, new UpdateCuratedPromptRequest("updated", "new content",
			List.of(PromptCategoryType.DEVELOPMENT, PromptCategoryType.TESTING), null, null));

		assertThat(existing.getDeletedAt()).isNull();
		assertThat(removed.getDeletedAt()).isNotNull();
		verify(promptCategoryRepository).saveAll(any());
		assertThat(prompt.getTitle()).isEqualTo("updated");
	}

	@Test
	void communityPromptCannotBeManagedByCuratedService() {
		when(promptRepository.findByIdAndProvenance(1L, PromptProvenance.CURATED)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(1L))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
	}
}
