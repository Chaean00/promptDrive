package com.chaean.promptdrive.member.internal.dto;

import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocialIdentityProfileResponse {

	private final SocialProvider provider;
	private final String providerUserId;
	private final String displayName;
	private final String verifiedEmail;

	public static SocialIdentityProfileResponse of(SocialProvider provider, String providerUserId, String displayName,
			String verifiedEmail) {
		return new SocialIdentityProfileResponse(provider, providerUserId, displayName, verifiedEmail);
	}
}
