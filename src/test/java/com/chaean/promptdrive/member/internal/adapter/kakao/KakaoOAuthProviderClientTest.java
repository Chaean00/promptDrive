package com.chaean.promptdrive.member.internal.adapter.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderProperties;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Kakao OAuth 외부 API adapter")
class KakaoOAuthProviderClientTest {

	private static final String TOKEN_URI = "https://kakao.test/token";
	private static final String USER_INFO_URI = "https://kakao.test/userinfo";

	private final MemberOAuthProperties properties = new MemberOAuthProperties();
	private MockRestServiceServer server;
	private KakaoOAuthProviderClient client;

	@BeforeEach
	void setUp() {
		OAuthProviderProperties kakao = properties.getKakao();
		kakao.setClientId("kakao-client");
		kakao.setClientSecret("kakao-secret");
		kakao.setRedirectUri("http://localhost:8080/api/auth/kakao/callback");
		kakao.setTokenUri(TOKEN_URI);
		kakao.setUserInfoUri(USER_INFO_URI);

		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new KakaoOAuthProviderClient(builder, properties);
	}

	@Test
	@DisplayName("state와 PKCE를 포함하고 nonce는 포함하지 않은 Kakao 인증 URL을 생성한다")
	void buildsAuthorizationUriWithStateAndPkce() {
		String uri = client.createAuthorizationUri("state", "challenge", "nonce");

		assertThat(uri).contains("client_id=kakao-client");
		assertThat(uri).contains("state=state");
		assertThat(uri).contains("code_challenge=challenge");
		assertThat(uri).contains("code_challenge_method=S256");
		assertThat(uri).doesNotContain("nonce=");
	}

	@Test
	@DisplayName("token과 사용자 정보를 조회하고 검증된 Kakao 프로필을 매핑한다")
	void exchangesCodeAndMapsKakaoProfileAndVerifiedEmail() {
		server.expect(requestTo(TOKEN_URI))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess("{\"access_token\":\"access-token\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(USER_INFO_URI))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer access-token"))
			.andRespond(withSuccess(
				"{\"id\":\"kakao-user\",\"kakao_account\":{\"profile\":{\"nickname\":\"카카오 사용자\"},\"email\":\"user@example.com\",\"is_email_valid\":true,\"is_email_verified\":true}}",
				MediaType.APPLICATION_JSON));

		var profile = client.authenticateUser("authorization-code", "verifier", null);

		assertThat(profile.getProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(profile.getProviderUserId()).isEqualTo("kakao-user");
		assertThat(profile.getDisplayName()).isEqualTo("카카오 사용자");
		assertThat(profile.getVerifiedEmail()).isEqualTo("user@example.com");
		server.verify();
	}
}
