package com.chaean.promptdrive.member.internal.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttempt;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttemptRepository;

import static org.mockito.Mockito.mock;

@DisplayName("OAuth 로그인 시도 상태 서비스")
class ConsumeOAuthLoginAttemptServiceTest {

	@Test
	@DisplayName("이미 사용한 state를 다시 사용하면 인증 오류를 반환한다")
	void rejectsReplayOfAnAlreadyConsumedState() {
		OAuthLoginAttemptRepository repository = mock(OAuthLoginAttemptRepository.class);
		OAuthLoginAttempt attempt = new OAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash", "encrypted-verifier", null,
				"/", Instant.now().plusSeconds(300));
		given(repository.findByStateHash("state-hash")).willReturn(Optional.of(attempt));
		ConsumeOAuthLoginAttemptService service = new ConsumeOAuthLoginAttemptService(repository);

		service.consumeOAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash");

		assertThatThrownBy(() -> service.consumeOAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash"))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.UNAUTHORIZED_REQUEST);
	}

	@Test
	@DisplayName("만료된 state를 사용하면 인증 오류를 반환한다")
	void rejectsAnExpiredState() {
		OAuthLoginAttemptRepository repository = mock(OAuthLoginAttemptRepository.class);
		OAuthLoginAttempt attempt = new OAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash", "encrypted-verifier", null,
				"/", Instant.now().minusSeconds(1));
		given(repository.findByStateHash("state-hash")).willReturn(Optional.of(attempt));
		ConsumeOAuthLoginAttemptService service = new ConsumeOAuthLoginAttemptService(repository);

		assertThatThrownBy(() -> service.consumeOAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash"))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.UNAUTHORIZED_REQUEST);
	}

	@Test
	@DisplayName("다른 provider에서 발급한 state를 사용하면 인증 오류를 반환한다")
	void rejectsAStateIssuedForAnotherProvider() {
		OAuthLoginAttemptRepository repository = mock(OAuthLoginAttemptRepository.class);
		OAuthLoginAttempt attempt = new OAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash", "encrypted-verifier", null,
				"/", Instant.now().plusSeconds(300));
		given(repository.findByStateHash("state-hash")).willReturn(Optional.of(attempt));
		ConsumeOAuthLoginAttemptService service = new ConsumeOAuthLoginAttemptService(repository);

		assertThatThrownBy(() -> service.consumeOAuthLoginAttempt(SocialProvider.KAKAO, "state-hash"))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(CommonErrorCode.UNAUTHORIZED_REQUEST);
		assertThat(attempt.getConsumedAt()).isNull();
	}
}
