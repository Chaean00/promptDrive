package com.chaean.promptdrive.member.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoAccountResponse {

	private KakaoProfileResponse profile;
	private String email;

	@JsonProperty("is_email_valid")
	private boolean emailValid;

	@JsonProperty("is_email_verified")
	private boolean emailVerified;
}
