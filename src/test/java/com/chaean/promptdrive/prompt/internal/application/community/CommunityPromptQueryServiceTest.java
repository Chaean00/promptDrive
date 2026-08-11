package com.chaean.promptdrive.prompt.internal.application.community;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.application.catalog.PromptResponseMapper;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCategoryRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("커뮤니티 Prompt 조회 서비스")
class CommunityPromptQueryServiceTest {

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptCategoryRepository promptCategoryRepository;

	private CommunityPromptQueryService service;

	@BeforeEach
	void setUp() {
		service = new CommunityPromptQueryService(promptRepository, promptCategoryRepository,
			new PromptResponseMapper());
	}

	@Test
	@DisplayName("다른 소유자와 큐레이션 Prompt는 조회할 수 없다")
	void hidesNonOwnedAndCuratedPrompts() {
		when(promptRepository.findByIdAndOwnerMemberIdAndProvenance(1L, 2L, PromptProvenance.COMMUNITY))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getOwnedCommunityPrompt(2L, 1L))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> org.assertj.core.api.Assertions.assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	@Test
	@DisplayName("잘못된 page 범위를 비즈니스 오류로 거부한다")
	void rejectsInvalidPageBounds() {
		assertThatThrownBy(() -> service.getOwnedCommunityPromptPage(1L, -1, 20))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> org.assertj.core.api.Assertions.assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST));
	}
}
