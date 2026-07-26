package com.chaean.promptdrive.member.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuthTokenResponse {

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("id_token")
	private String idToken;
}
