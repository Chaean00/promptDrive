package com.chaean.promptdrive.member.internal.application;

import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.domain.MemberRole;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.MemberRepository;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentity;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentityRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindOrCreateSocialLoginMemberService {

	private final MemberRepository memberRepository;
	private final SocialIdentityRepository socialIdentityRepository;

	@Transactional
	public Member findOrCreateSocialLoginMember(SocialIdentityProfileResponse profile) {
		return socialIdentityRepository.findByProviderAndProviderUserId(profile.getProvider().name(), profile.getProviderUserId())
				.map(SocialIdentity::getMember)
				.orElseGet(() -> createMemberFromSocialProfile(profile));
	}

	private Member createMemberFromSocialProfile(SocialIdentityProfileResponse profile) {
		Member member = memberRepository.save(new Member(resolveMemberNickname(profile), MemberRole.MEMBER));
		socialIdentityRepository.save(new SocialIdentity(member, profile.getProvider().name(), profile.getProviderUserId(),
				profile.getVerifiedEmail()));
		return member;
	}

	private String resolveMemberNickname(SocialIdentityProfileResponse profile) {
		String value = profile.getDisplayName();
		if (value == null || value.isBlank()) {
			value = profile.getProvider().getCode() + "-" + profile.getProviderUserId();
		}
		return value.length() <= 100 ? value : value.substring(0, 100);
	}
}
