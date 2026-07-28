package com.chaean.promptdrive.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.MemberRepository;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentity;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentityRepository;

@DisplayName("소셜 로그인 회원 서비스")
class FindOrCreateSocialLoginMemberServiceTest {

	@Test
	@DisplayName("같은 이메일이어도 provider가 다르면 별도 회원을 생성한다")
	void createsSeparateMembersForDifferentProvidersWithTheSameEmail() {
		MemberRepository memberRepository = mock(MemberRepository.class);
		SocialIdentityRepository identityRepository = mock(SocialIdentityRepository.class);
		given(identityRepository.findByProviderAndProviderUserId(any(), any())).willReturn(Optional.empty());
		given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));
		FindOrCreateSocialLoginMemberService service = new FindOrCreateSocialLoginMemberService(memberRepository, identityRepository);

		Member googleMember = service.findOrCreateSocialLoginMember(SocialIdentityProfileResponse.of(SocialProvider.GOOGLE, "google-1", "Google", "same@example.com"));
		Member kakaoMember = service.findOrCreateSocialLoginMember(SocialIdentityProfileResponse.of(SocialProvider.KAKAO, "kakao-1", "Kakao", "same@example.com"));

		assertThat(googleMember).isNotSameAs(kakaoMember);
	}
}
