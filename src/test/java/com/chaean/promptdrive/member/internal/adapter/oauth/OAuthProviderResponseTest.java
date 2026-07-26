package com.chaean.promptdrive.member.internal.adapter.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaean.promptdrive.member.internal.dto.GoogleUserInfoResponse;
import com.chaean.promptdrive.member.internal.dto.KakaoUserInfoResponse;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class OAuthProviderResponseTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void mapsGoogleSnakeCaseUserInfo() throws Exception {
		GoogleUserInfoResponse response = objectMapper.readValue(
				"{\"sub\":\"google-user\",\"email\":\"user@example.com\",\"email_verified\":true}",
				GoogleUserInfoResponse.class);

		assertThat(response.getSub()).isEqualTo("google-user");
		assertThat(response.isEmailVerified()).isTrue();
	}

	@Test
	void mapsKakaoSnakeCaseUserInfo() throws Exception {
		KakaoUserInfoResponse response = objectMapper.readValue(
				"{\"id\":\"kakao-user\",\"kakao_account\":{\"profile\":{\"nickname\":\"사용자\"}}}",
				KakaoUserInfoResponse.class);

		assertThat(response.getId()).isEqualTo("kakao-user");
		assertThat(response.getKakaoAccount()).isNotNull();
		assertThat(response.getKakaoAccount().getProfile().getNickname()).isEqualTo("사용자");
	}
}
