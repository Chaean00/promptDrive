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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("큐레이션 Prompt 관리 서비스")
class CuratedPromptCommandServiceTest {

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptCategoryRepository promptCategoryRepository;

	private CuratedPromptCommandService service;

	@BeforeEach
	void setUp() {
		service = new CuratedPromptCommandService(promptRepository, promptCategoryRepository,
			new PromptResponseMapper());
	}

	@Test
	@DisplayName("중복 category를 비즈니스 오류로 거부한다")
	void rejectsDuplicateCategoriesAsBusinessError() {
		CreateCuratedPromptRequest request = new CreateCuratedPromptRequest(
			"title", "content", List.of(PromptCategoryType.DEVELOPMENT, PromptCategoryType.DEVELOPMENT),
			PromptVisibility.PUBLIC, null, null);

		assertThatThrownBy(() -> service.createCuratedPrompt(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
		verify(promptRepository, never()).save(any());
	}

	@Test
	@DisplayName("null category를 비즈니스 오류로 거부한다")
	void rejectsNullCategoryAsBusinessError() {
		CreateCuratedPromptRequest request = new CreateCuratedPromptRequest(
			"title", "content", java.util.Arrays.asList(PromptCategoryType.DEVELOPMENT, null),
			PromptVisibility.PUBLIC, null, null);

		assertThatThrownBy(() -> service.createCuratedPrompt(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
	}

	@Test
	@DisplayName("큐레이션 Prompt와 활성 category를 함께 삭제한다")
	void deletesCuratedPromptAndAllActiveCategories() {
		Prompt prompt = Prompt.createCuratedPrompt("title", "content", PromptVisibility.PUBLIC, null, null);
		PromptCategory category = PromptCategory.createPromptCategory(prompt, PromptCategoryType.DEVELOPMENT);
		when(promptRepository.findByIdAndProvenanceForUpdate(1L, PromptProvenance.CURATED)).thenReturn(Optional.of(prompt));
		when(promptCategoryRepository.findAllByPromptId(1L)).thenReturn(List.of(category));

		service.deleteCuratedPrompt(1L);

		assertThat(prompt.getDeletedAt()).isNotNull();
		assertThat(category.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("기존 category는 유지하고 새로운 category만 추가해 Prompt를 수정한다")
	void updatesCuratedPromptByPreservingExistingAndAddingOnlyNewCategories() {
		Prompt prompt = Prompt.createCuratedPrompt("title", "content", PromptVisibility.PUBLIC, null, null);
		PromptCategory existing = PromptCategory.createPromptCategory(prompt, PromptCategoryType.DEVELOPMENT);
		PromptCategory removed = PromptCategory.createPromptCategory(prompt, PromptCategoryType.CODE_REVIEW);
		when(promptRepository.findByIdAndProvenanceForUpdate(1L, PromptProvenance.CURATED)).thenReturn(Optional.of(prompt));
		when(promptCategoryRepository.findAllByPromptId(1L)).thenReturn(List.of(existing, removed));

		service.updateCuratedPrompt(1L, new UpdateCuratedPromptRequest("updated", "new content",
			List.of(PromptCategoryType.DEVELOPMENT, PromptCategoryType.TESTING), null, null));

		assertThat(existing.getDeletedAt()).isNull();
		assertThat(removed.getDeletedAt()).isNotNull();
		verify(promptCategoryRepository).saveAll(any());
		assertThat(prompt.getTitle()).isEqualTo("updated");
	}

}
