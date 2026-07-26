package com.chaean.promptdrive.member.internal.application;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenPair {

	private final String accessToken;
	private final String refreshToken;
	private final Duration refreshTokenTtl;

	public static TokenPair of(String accessToken, String refreshToken, Duration refreshTokenTtl) {
		return new TokenPair(accessToken, refreshToken, refreshTokenTtl);
	}
}
