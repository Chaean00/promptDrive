package com.chaean.promptdrive.member.internal.application;

import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import org.springframework.dao.DataIntegrityViolationException;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentity;
import com.chaean.promptdrive.member.internal.persistence.SocialIdentityRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialLoginMemberCommandService {

	private final SocialIdentityRepository socialIdentityRepository;
	private final SocialLoginMemberCreationService socialLoginMemberCreationService;

	public Member getOrCreateSocialLoginMember(SocialIdentityProfileResponse profile) {
		return socialIdentityRepository.findByProviderAndProviderUserId(profile.getProvider().name(), profile.getProviderUserId())
				.map(SocialIdentity::getMember)
				.orElseGet(() -> createOrLoadSocialLoginMember(profile));
	}

	private Member createOrLoadSocialLoginMember(SocialIdentityProfileResponse profile) {
		try {
			return socialLoginMemberCreationService.createSocialLoginMember(profile);
		} catch (DataIntegrityViolationException exception) {
			return socialIdentityRepository.findByProviderAndProviderUserId(profile.getProvider().name(), profile.getProviderUserId())
					.map(SocialIdentity::getMember)
					.orElseThrow(() -> exception);
		}
	}
}
