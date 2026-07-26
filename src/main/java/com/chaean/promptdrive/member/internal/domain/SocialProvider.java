package com.chaean.promptdrive.member.internal.domain;

import java.util.Locale;
import java.util.Arrays;
import java.util.Optional;

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

	public static Optional<SocialProvider> from(String value) {
		if (value == null) {
			return Optional.empty();
		}

		return Arrays.stream(values())
				.filter(provider -> provider.name().equalsIgnoreCase(value)
						|| provider.code.equals(value.toLowerCase(Locale.ROOT)))
				.findFirst();
	}
}
