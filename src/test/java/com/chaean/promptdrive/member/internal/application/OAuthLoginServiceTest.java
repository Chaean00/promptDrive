package com.chaean.promptdrive.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderClient;
import com.chaean.promptdrive.member.internal.adapter.oauth.PkceStateCipher;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.dto.OAuthLoginResponse;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.dto.TokenPairResponse;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttempt;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttemptRepository;
import com.chaean.promptdrive.member.internal.util.OAuthSecurityValueGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@DisplayName("OAuth 로그인 서비스")
class OAuthLoginServiceTest {

	private final OAuthProviderClient providerClient = mock(OAuthProviderClient.class);
	private final OAuthLoginAttemptRepository attemptRepository = mock(OAuthLoginAttemptRepository.class);
	private final OAuthLoginAttemptService attemptService = mock(OAuthLoginAttemptService.class);
	private final SocialLoginMembershipService membershipService = mock(SocialLoginMembershipService.class);
	private final PkceStateCipher stateCipher = mock(PkceStateCipher.class);
	private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
	private final MemberOAuthProperties properties = new MemberOAuthProperties();
	private final OAuthSecurityValueGenerator valueGenerator = mock(OAuthSecurityValueGenerator.class);
	private OAuthLoginService service;

	@BeforeEach
	void setUp() {
		properties.setAllowedReturnPaths(List.of("/", "/dashboard"));
		given(providerClient.provider()).willReturn(SocialProvider.GOOGLE);
		service = new OAuthLoginService(List.of(providerClient), attemptRepository, attemptService, membershipService,
			stateCipher, refreshTokenService, properties, valueGenerator);
	}

	@Test
	@DisplayName("허용된 return path와 state·PKCE·nonce를 생성해 로그인을 시작한다")
	void startsLoginWithStatePkceNonceAndAllowedReturnPath() {
		given(valueGenerator.generate()).willReturn("state", "verifier", "nonce");
		given(valueGenerator.sha256("state")).willReturn("state-hash");
		given(valueGenerator.sha256("nonce")).willReturn("nonce-hash");
		given(valueGenerator.pkceChallenge("verifier")).willReturn("challenge");
		given(stateCipher.encrypt("verifier")).willReturn("encrypted-verifier");
		given(providerClient.authorizationUri("state", "challenge", "nonce"))
			.willReturn("https://accounts.google.com/auth?state=state");

		var response = service.start(SocialProvider.GOOGLE, "/dashboard");

		assertThat(response.getAuthorizationUri()).isEqualTo("https://accounts.google.com/auth?state=state");
		assertThat(response.getState()).isEqualTo("state");
		verify(attemptRepository).save(any(OAuthLoginAttempt.class));
		verify(providerClient).authorizationUri("state", "challenge", "nonce");
	}

	@Test
	@DisplayName("허용 목록에 없는 return path를 거부한다")
	void rejectsReturnPathOutsideAllowList() {
		assertThatThrownBy(() -> service.start(SocialProvider.GOOGLE, "https://evil.example"))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(CommonErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("authorization code가 비어 있으면 state를 소비하기 전에 거부한다")
	void rejectsBlankAuthorizationCodeBeforeConsumingState() {
		assertThatThrownBy(() -> service.callback(SocialProvider.GOOGLE, " ", "state"))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(CommonErrorCode.UNAUTHORIZED_REQUEST);

		org.mockito.Mockito.verifyNoInteractions(attemptService);
	}

	@Test
	@DisplayName("state를 소비하고 PKCE verifier를 복호화한 뒤 회원과 token을 발급한다")
	void consumesStateDecryptsVerifierAndIssuesTokensAfterProviderAuthentication() {
		OAuthLoginAttempt attempt = new OAuthLoginAttempt(SocialProvider.GOOGLE, "state-hash", "encrypted-verifier",
			"nonce-hash", "/dashboard", Instant.now().plusSeconds(300));
		Member member = mock(Member.class);
		TokenPairResponse tokens = TokenPairResponse.of("access-token", "refresh-token", java.time.Duration.ofDays(30));
		SocialIdentityProfileResponse profile = SocialIdentityProfileResponse.of(SocialProvider.GOOGLE, "google-user", "User", null);
		given(valueGenerator.sha256("state")).willReturn("state-hash");
		given(attemptService.consume(SocialProvider.GOOGLE, "state-hash")).willReturn(attempt);
		given(stateCipher.decrypt("encrypted-verifier")).willReturn("verifier");
		given(providerClient.authenticate("authorization-code", "verifier", "nonce-hash")).willReturn(profile);
		given(membershipService.findOrCreate(profile)).willReturn(member);
		given(refreshTokenService.issue(member)).willReturn(tokens);

		OAuthLoginResponse response = service.callback(SocialProvider.GOOGLE, "authorization-code", "state");

		assertThat(response.getReturnPath()).isEqualTo("/dashboard");
		assertThat(response.getTokens()).isSameAs(tokens);
		verify(providerClient).authenticate("authorization-code", "verifier", "nonce-hash");
		verify(refreshTokenService).issue(member);
	}
}
