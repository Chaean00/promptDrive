package com.chaean.promptdrive.member.internal.application;

import com.chaean.promptdrive.member.internal.domain.MemberRole;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.MemberRepository;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentity;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentityRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SocialLoginMemberCreationService {

	private final MemberRepository memberRepository;
	private final SocialIdentityRepository socialIdentityRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Member createSocialLoginMember(SocialIdentityProfileResponse profile) {
		Member member = memberRepository.save(Member.create(resolveMemberNickname(profile), MemberRole.MEMBER));
		socialIdentityRepository.saveAndFlush(SocialIdentity.create(member, profile.getProvider(), profile.getProviderUserId(),
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
