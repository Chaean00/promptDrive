package com.chaean.promptdrive.member.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserInfoResponse {

	private String sub;
	private String name;
	private String email;
	@JsonProperty("email_verified")
	private boolean emailVerified;
}
