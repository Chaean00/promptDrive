package com.chaean.promptdrive.member.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoResponse {

	private String id;
	@JsonProperty("kakao_account")
	private KakaoAccountResponse kakaoAccount;
}
