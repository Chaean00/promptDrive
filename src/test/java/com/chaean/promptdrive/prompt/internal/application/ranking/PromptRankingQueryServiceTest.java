package com.chaean.promptdrive.prompt.internal.application.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.projection.PromptRankingProjection;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Prompt ranking 조회 서비스")
class PromptRankingQueryServiceTest {

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptCategoryRepository promptCategoryRepository;

	private PromptRankingQueryService service;

	@BeforeEach
	void setUp() {
		service = new PromptRankingQueryService(promptRepository, promptCategoryRepository,
			new PromptResponseMapper());
	}

	@Test
	@DisplayName("page와 size 기본값으로 ranking을 조회하고 category를 한 번에 조회한다")
	void usesDefaultsAndLoadsCategoriesInBatch() {
		PromptRankingProjection first = ranking(1L, 5L);
		PromptRankingProjection second = ranking(2L, 0L);
		when(promptRepository.findPublicPromptRankings(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 20))))
			.thenReturn(new SliceImpl<>(List.of(first, second), PageRequest.of(0, 20), true));
		when(promptCategoryRepository.findAllByPromptIdIn(List.of(1L, 2L))).thenReturn(List.of());

		var response = service.getPromptRankings("all", null, null);

		assertThat(response.getPage()).isZero();
		assertThat(response.getSize()).isEqualTo(20);
		assertThat(response.getContent()).hasSize(2);
		assertThat(response.isFirst()).isTrue();
		assertThat(response.isLast()).isFalse();
		assertThat(response.isHasNext()).isTrue();
		verify(promptCategoryRepository).findAllByPromptIdIn(List.of(1L, 2L));
	}

	@Test
	@DisplayName("지정한 page와 size를 PageRequest에 전달한다")
	void usesRequestedPageAndSize() {
		when(promptRepository.findPublicPromptRankings(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(PageRequest.of(2, 3))))
			.thenReturn(new SliceImpl<>(List.<PromptRankingProjection>of(), PageRequest.of(2, 3), false));

		var response = service.getPromptRankings("all", 2, 3);

		assertThat(response.getPage()).isEqualTo(2);
		assertThat(response.getSize()).isEqualTo(3);
		verify(promptCategoryRepository, never()).findAllByPromptIdIn(any());
	}

	@Test
	@DisplayName("page와 size의 경계를 벗어난 요청을 거부한다")
	void rejectsInvalidBounds() {
		for (var arguments : List.of(
			new Integer[] {-1, 1},
			new Integer[] {0, 0},
			new Integer[] {0, 101})) {
			assertThatThrownBy(() -> service.getPromptRankings("all", arguments[0], arguments[1]))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
					.isEqualTo(CommonErrorCode.INVALID_REQUEST));
		}
		verify(promptRepository, never()).findPublicPromptRankings(any(), any());
	}

	private PromptRankingProjection ranking(long promptId, long likeCount) {
		Prompt prompt = org.mockito.Mockito.mock(Prompt.class);
		when(prompt.getId()).thenReturn(promptId);
		when(prompt.getTitle()).thenReturn("title-" + promptId);
		when(prompt.getProvenance()).thenReturn(PromptProvenance.CURATED);
		PromptRankingProjection ranking = org.mockito.Mockito.mock(PromptRankingProjection.class);
		when(ranking.getPrompt()).thenReturn(prompt);
		when(ranking.getLikeCount()).thenReturn(likeCount);
		return ranking;
	}
}
