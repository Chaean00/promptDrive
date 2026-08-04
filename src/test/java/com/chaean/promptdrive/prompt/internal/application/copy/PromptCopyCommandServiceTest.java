package com.chaean.promptdrive.prompt.internal.application.copy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptCopyCommandServiceTest {
	@Mock private PromptRepository promptRepository;
	private PromptCopyCommandService service;

	@BeforeEach
	void setUp() { service = new PromptCopyCommandService(promptRepository); }

	@Test
	void returnsPersistedCountAfterIncrement() {
		Prompt prompt = org.mockito.Mockito.mock(Prompt.class);
		when(promptRepository.incrementPublicCopyCount(1L)).thenReturn(1);
		when(promptRepository.findByIdAndVisibility(1L, com.chaean.promptdrive.prompt.internal.domain.PromptVisibility.PUBLIC)).thenReturn(Optional.of(prompt));
		when(prompt.getCopyCount()).thenReturn(1L);

		var response = service.registerPromptCopy(1L);

		assertThat(response.getPromptId()).isEqualTo(1L);
		assertThat(response.getCopyCount()).isEqualTo(1L);
		verify(promptRepository).incrementPublicCopyCount(1L);
	}

	@Test
	void rejectsZeroRowUpdateAsNotFound() {
		when(promptRepository.incrementPublicCopyCount(1L)).thenReturn(0);
		assertThatThrownBy(() -> service.registerPromptCopy(1L))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
				.isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
	}
}
