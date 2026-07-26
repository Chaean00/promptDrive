package com.chaean.promptdrive.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.MemberRepository;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentity;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentityRepository;

class SocialLoginMembershipServiceTest {

	@Test
	void createsSeparateMembersForDifferentProvidersWithTheSameEmail() {
		MemberRepository memberRepository = mock(MemberRepository.class);
		SocialIdentityRepository identityRepository = mock(SocialIdentityRepository.class);
		given(identityRepository.findByProviderAndProviderUserId(any(), any())).willReturn(Optional.empty());
		given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));
		SocialLoginMembershipService service = new SocialLoginMembershipService(memberRepository, identityRepository);

		Member googleMember = service.findOrCreate(SocialIdentityProfileResponse.of(SocialProvider.GOOGLE, "google-1", "Google", "same@example.com"));
		Member kakaoMember = service.findOrCreate(SocialIdentityProfileResponse.of(SocialProvider.KAKAO, "kakao-1", "Kakao", "same@example.com"));

		assertThat(googleMember).isNotSameAs(kakaoMember);
	}
}
