package com.chaean.promptdrive.member.internal.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthLoginStartResponse {

	private final String authorizationUri;
	private final String state;

	public static OAuthLoginStartResponse of(String authorizationUri, String state) {
		return new OAuthLoginStartResponse(authorizationUri, state);
	}
}
