package com.chaean.promptdrive.member.internal.domain;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProvider {

	GOOGLE("google", "Google", "구글"),
	KAKAO("kakao", "Kakao", "카카오");

	private final String code;
	private final String englishName;
	private final String koreanName;

	public static SocialProvider from(String value) {
		return SocialProvider.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
