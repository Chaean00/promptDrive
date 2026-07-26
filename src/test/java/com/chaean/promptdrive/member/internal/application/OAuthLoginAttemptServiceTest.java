package com.chaean.promptdrive.member.internal.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttempt;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttemptRepository;

import static org.mockito.Mockito.mock;

class OAuthLoginAttemptServiceTest {

	@Test
	void rejectsReplayOfAnAlreadyConsumedState() {
		OAuthLoginAttemptRepository repository = mock(OAuthLoginAttemptRepository.class);
		OAuthLoginAttempt attempt = new OAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash", "encrypted-verifier", null,
				"/", Instant.now().plusSeconds(300));
		given(repository.findByStateHash("state-hash")).willReturn(Optional.of(attempt));
		OAuthLoginAttemptService service = new OAuthLoginAttemptService(repository);

		service.consume(SocialProvider.GOOGLE, "state-hash");

		assertThatThrownBy(() -> service.consume(SocialProvider.GOOGLE, "state-hash"))
				.isInstanceOf(ResponseStatusException.class);
	}
}
