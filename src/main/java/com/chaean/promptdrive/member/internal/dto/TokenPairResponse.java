package com.chaean.promptdrive.member.internal.dto;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenPairResponse {

	private final String accessToken;
	private final String refreshToken;
	private final Duration refreshTokenTtl;

	public static TokenPairResponse of(String accessToken, String refreshToken, Duration refreshTokenTtl) {
		return new TokenPairResponse(accessToken, refreshToken, refreshTokenTtl);
	}
}
