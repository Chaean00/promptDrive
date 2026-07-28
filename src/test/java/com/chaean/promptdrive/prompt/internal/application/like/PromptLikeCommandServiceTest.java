package com.chaean.promptdrive.prompt.internal.application.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptLike;
import com.chaean.promptdrive.prompt.internal.persistence.PromptLikeRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Prompt 좋아요 관리 서비스")
class PromptLikeCommandServiceTest {

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptLikeRepository promptLikeRepository;

	private PromptLikeCommandService service;

	@BeforeEach
	void setUp() {
		service = new PromptLikeCommandService(promptRepository, promptLikeRepository);
	}

	@Test
	@DisplayName("공개 Prompt에 좋아요를 등록한다")
	void likesPublicPrompt() {
		when(promptRepository.findByIdAndVisibility(1L, PromptVisibility.PUBLIC)).thenReturn(Optional.of(publicPrompt()));
		when(promptLikeRepository.reactivateLatestDeletedByPromptIdAndMemberId(1L, 7L)).thenReturn(0);

		var response = service.registerPromptLike(7L, 1L);

		assertThat(response.getPromptId()).isEqualTo(1L);
		assertThat(response.isLiked()).isTrue();
		verify(promptLikeRepository).insertIfAbsent(1L, 7L);
	}

	@Test
	@DisplayName("중복 좋아요는 원자적 삽입으로 기존 활성 행을 유지하는 멱등 동작이다")
	void duplicateLikeIsIdempotent() {
		when(promptRepository.findByIdAndVisibility(1L, PromptVisibility.PUBLIC)).thenReturn(Optional.of(publicPrompt()));
		when(promptLikeRepository.reactivateLatestDeletedByPromptIdAndMemberId(1L, 7L)).thenReturn(0);

		assertThat(service.registerPromptLike(7L, 1L).isLiked()).isTrue();

		verify(promptLikeRepository).insertIfAbsent(1L, 7L);
	}

	@Test
	@DisplayName("기존 소프트 삭제 좋아요를 재활성화한다")
	void restoresDeletedLike() {
		when(promptRepository.findByIdAndVisibility(1L, PromptVisibility.PUBLIC)).thenReturn(Optional.of(publicPrompt()));
		when(promptLikeRepository.reactivateLatestDeletedByPromptIdAndMemberId(1L, 7L)).thenReturn(1);

		assertThat(service.registerPromptLike(7L, 1L).isLiked()).isTrue();

		verify(promptLikeRepository, never()).insertIfAbsent(any(), any());
	}

	@Test
	@DisplayName("좋아요 취소는 활성 행을 소프트 삭제하고 중복 취소는 무시한다")
	void unlikesIdempotently() {
		Prompt prompt = publicPrompt();
		PromptLike existing = new PromptLike(prompt, 7L);
		when(promptRepository.findByIdAndVisibility(1L, PromptVisibility.PUBLIC)).thenReturn(Optional.of(prompt));
		when(promptLikeRepository.findByPromptIdAndMemberId(1L, 7L))
			.thenReturn(Optional.of(existing))
			.thenReturn(Optional.empty());

		assertThat(service.removePromptLike(7L, 1L).isLiked()).isFalse();
		assertThat(existing.getDeletedAt()).isNotNull();
		assertThat(service.removePromptLike(7L, 1L).isLiked()).isFalse();
	}

	@Test
	@DisplayName("비공개 Prompt에는 좋아요를 변경할 수 없다")
	void rejectsNonPublicPrompt() {
		when(promptRepository.findByIdAndVisibility(1L, PromptVisibility.PUBLIC)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.registerPromptLike(7L, 1L))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
		verify(promptLikeRepository, never()).reactivateLatestDeletedByPromptIdAndMemberId(any(), any());
		verify(promptLikeRepository, never()).insertIfAbsent(any(), any());
	}

	@Test
	@DisplayName("없는 Prompt에는 좋아요를 변경할 수 없다")
	void rejectsMissingPrompt() {
		when(promptRepository.findByIdAndVisibility(1L, PromptVisibility.PUBLIC)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.removePromptLike(7L, 1L))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	private Prompt publicPrompt() {
		return Prompt.createCuratedPrompt("title", "content", PromptVisibility.PUBLIC, null, null);
	}
}
