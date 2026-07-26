package com.chaean.promptdrive.member.internal.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthLoginResponse {

	private final TokenPairResponse tokens;
	private final String returnPath;

	public static OAuthLoginResponse of(TokenPairResponse tokens, String returnPath) {
		return new OAuthLoginResponse(tokens, returnPath);
	}
}
