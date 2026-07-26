package com.chaean.promptdrive.member.internal.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthTokenResponse {

	private final String accessToken;
	private final String tokenType;
	private final String returnPath;

	public static AuthTokenResponse of(String accessToken, String returnPath) {
		return new AuthTokenResponse(accessToken, "Bearer", returnPath);
	}
}
