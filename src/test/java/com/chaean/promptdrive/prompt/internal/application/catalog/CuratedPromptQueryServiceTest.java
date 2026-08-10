package com.chaean.promptdrive.prompt.internal.application.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
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
@DisplayName("큐레이션 Prompt 조회 서비스")
class CuratedPromptQueryServiceTest {

	@Mock
	private PromptRepository promptRepository;

	@Mock
	private PromptCategoryRepository promptCategoryRepository;

	private CuratedPromptQueryService service;

	@BeforeEach
	void setUp() {
		service = new CuratedPromptQueryService(promptRepository, promptCategoryRepository,
			new PromptResponseMapper());
	}

	@Test
	@DisplayName("커뮤니티 Prompt는 큐레이션 조회 서비스에서 찾을 수 없다")
	void communityPromptCannotBeReadAsCuratedPrompt() {
		when(promptRepository.findByIdAndProvenance(1L, PromptProvenance.CURATED)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getCuratedPrompt(1L))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> org.assertj.core.api.Assertions.assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
	}
}
