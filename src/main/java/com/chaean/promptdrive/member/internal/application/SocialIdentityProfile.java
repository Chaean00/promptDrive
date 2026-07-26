package com.chaean.promptdrive.member.internal.application;

import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocialIdentityProfile {

	private final SocialProvider provider;
	private final String providerUserId;
	private final String displayName;
	private final String verifiedEmail;

	public static SocialIdentityProfile of(SocialProvider provider, String providerUserId, String displayName,
			String verifiedEmail) {
		return new SocialIdentityProfile(provider, providerUserId, displayName, verifiedEmail);
	}
}
