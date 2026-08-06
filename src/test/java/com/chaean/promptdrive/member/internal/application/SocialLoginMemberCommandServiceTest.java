package com.chaean.promptdrive.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.dao.DataIntegrityViolationException;

import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.domain.MemberRole;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentity;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentityRepository;

@DisplayName("소셜 로그인 회원 서비스")
class SocialLoginMemberCommandServiceTest {

	@Test
	@DisplayName("같은 이메일이어도 provider가 다르면 별도 회원을 생성한다")
	void createsSeparateMembersForDifferentProvidersWithTheSameEmail() {
		SocialIdentityRepository identityRepository = mock(SocialIdentityRepository.class);
		SocialLoginMemberCreationService creationService = mock(SocialLoginMemberCreationService.class);
		given(identityRepository.findByProviderAndProviderUserId(any(), any())).willReturn(Optional.empty());
		given(creationService.createSocialLoginMember(any(SocialIdentityProfileResponse.class)))
			.willAnswer(invocation -> Member.create("member", MemberRole.MEMBER));
		SocialLoginMemberCommandService service = new SocialLoginMemberCommandService(identityRepository, creationService);

		Member googleMember = service.getOrCreateSocialLoginMember(SocialIdentityProfileResponse.of(SocialProvider.GOOGLE, "google-1", "Google", "same@example.com"));
		Member kakaoMember = service.getOrCreateSocialLoginMember(SocialIdentityProfileResponse.of(SocialProvider.KAKAO, "kakao-1", "Kakao", "same@example.com"));

		assertThat(googleMember).isNotSameAs(kakaoMember);
	}

	@Test
	@DisplayName("동일 소셜 계정 생성 경합에서 이미 생성된 회원을 반환한다")
	void returnsExistingMemberWhenSocialIdentityCreationRaces() {
		SocialIdentityRepository identityRepository = mock(SocialIdentityRepository.class);
		SocialLoginMemberCreationService creationService = mock(SocialLoginMemberCreationService.class);
		Member existingMember = mock(Member.class);
		SocialIdentity existingIdentity = mock(SocialIdentity.class);
		given(existingIdentity.getMember()).willReturn(existingMember);
		given(identityRepository.findByProviderAndProviderUserId("GOOGLE", "google-1"))
			.willReturn(Optional.empty())
			.willReturn(Optional.of(existingIdentity));
		given(creationService.createSocialLoginMember(any(SocialIdentityProfileResponse.class)))
			.willThrow(new DataIntegrityViolationException("duplicate social identity"));
		SocialLoginMemberCommandService service = new SocialLoginMemberCommandService(identityRepository, creationService);

		Member result = service.getOrCreateSocialLoginMember(
			SocialIdentityProfileResponse.of(SocialProvider.GOOGLE, "google-1", "Google", "user@example.com"));

		assertThat(result).isSameAs(existingMember);
	}
}
