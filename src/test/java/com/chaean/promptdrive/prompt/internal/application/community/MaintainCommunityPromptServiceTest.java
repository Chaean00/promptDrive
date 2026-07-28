package com.chaean.promptdrive.prompt.internal.application.community;

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
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCommunityPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.UpdateCommunityPromptRequest;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategory;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("커뮤니티 Prompt 관리 서비스")
class MaintainCommunityPromptServiceTest {

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptCategoryRepository promptCategoryRepository;

	private MaintainCommunityPromptService service;

	@BeforeEach
	void setUp() {
		service = new MaintainCommunityPromptService(promptRepository, promptCategoryRepository,
			new PromptResponseMapper());
	}

	@Test
	@DisplayName("소유자 Prompt를 COMMUNITY와 PUBLIC으로 생성한다")
	void createsPublicCommunityPromptForOwner() {
		when(promptRepository.save(any(Prompt.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(promptCategoryRepository.findAllByPromptId(null)).thenReturn(List.of());

		service.create(1L, new CreateCommunityPromptRequest("title", "content", List.of(PromptCategoryType.DEVELOPMENT)));

		org.mockito.ArgumentCaptor<Prompt> promptCaptor = org.mockito.ArgumentCaptor.forClass(Prompt.class);
		verify(promptRepository).save(promptCaptor.capture());
		assertThat(promptCaptor.getValue().getOwnerMemberId()).isEqualTo(1L);
		assertThat(promptCaptor.getValue().getProvenance()).isEqualTo(PromptProvenance.COMMUNITY);
		assertThat(promptCaptor.getValue().getVisibility()).isEqualTo(PromptVisibility.PUBLIC);
	}

	@Test
	@DisplayName("중복 category를 비즈니스 오류로 거부한다")
	void rejectsDuplicateCategories() {
		CreateCommunityPromptRequest request = new CreateCommunityPromptRequest("title", "content",
			List.of(PromptCategoryType.DEVELOPMENT, PromptCategoryType.DEVELOPMENT));

		assertThatThrownBy(() -> service.create(1L, request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
		verify(promptRepository, never()).save(any());
	}

	@Test
	@DisplayName("다른 소유자와 큐레이션 Prompt는 찾을 수 없다")
	void hidesNonOwnedAndCuratedPrompts() {
		when(promptRepository.findByIdAndOwnerMemberIdAndProvenance(1L, 2L, PromptProvenance.COMMUNITY))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(2L, 1L))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	@Test
	@DisplayName("기존 category는 유지하고 새 category만 추가해 수정한다")
	void updatesByCategorySetDifference() {
		Prompt prompt = Prompt.createCommunity(1L, "title", "content");
		PromptCategory existing = PromptCategory.create(prompt, PromptCategoryType.DEVELOPMENT);
		PromptCategory removed = PromptCategory.create(prompt, PromptCategoryType.CODE_REVIEW);
		when(promptRepository.findByIdAndOwnerMemberIdAndProvenance(1L, 1L, PromptProvenance.COMMUNITY))
			.thenReturn(Optional.of(prompt));
		when(promptCategoryRepository.findAllByPromptId(1L)).thenReturn(List.of(existing, removed));

		service.update(1L, 1L, new UpdateCommunityPromptRequest("updated", "new content",
			List.of(PromptCategoryType.DEVELOPMENT, PromptCategoryType.TESTING)));

		assertThat(existing.getDeletedAt()).isNull();
		assertThat(removed.getDeletedAt()).isNotNull();
		assertThat(prompt.getTitle()).isEqualTo("updated");
		verify(promptCategoryRepository).saveAll(any());
	}

	@Test
	@DisplayName("Prompt와 활성 category를 함께 soft delete한다")
	void softDeletesOwnedPromptAndCategories() {
		Prompt prompt = Prompt.createCommunity(1L, "title", "content");
		PromptCategory category = PromptCategory.create(prompt, PromptCategoryType.DEVELOPMENT);
		when(promptRepository.findByIdAndOwnerMemberIdAndProvenance(1L, 1L, PromptProvenance.COMMUNITY))
			.thenReturn(Optional.of(prompt));
		when(promptCategoryRepository.findAllByPromptId(1L)).thenReturn(List.of(category));

		service.delete(1L, 1L);

		assertThat(prompt.getDeletedAt()).isNotNull();
		assertThat(category.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("잘못된 page 범위를 비즈니스 오류로 거부한다")
	void rejectsInvalidPageBounds() {
		assertThatThrownBy(() -> service.browse(1L, -1, 20))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
	}
}
