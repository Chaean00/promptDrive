package com.chaean.promptdrive.member.internal.adapter.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderProperties;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.stub.member.JwtDecoderProviderStub;
import com.chaean.promptdrive.member.internal.util.OAuthSecurityValueGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Google OAuth 외부 API adapter")
class GoogleOAuthProviderClientTest {

	private static final String CLIENT_ID = "google-client";
	private static final String TOKEN_URI = "https://google.test/token";
	private static final String USER_INFO_URI = "https://google.test/userinfo";

	private final MemberOAuthProperties properties = new MemberOAuthProperties();
	private final OAuthSecurityValueGenerator valueGenerator = new OAuthSecurityValueGenerator();
	private final JwtDecoderProviderStub decoderProvider = new JwtDecoderProviderStub();
	private MockRestServiceServer server;
	private GoogleOAuthProviderClient client;

	@BeforeEach
	void setUp() {
		OAuthProviderProperties google = properties.getGoogle();
		google.setClientId(CLIENT_ID);
		google.setClientSecret("google-secret");
		google.setRedirectUri("http://localhost:8080/api/auth/google/callback");
		google.setTokenUri(TOKEN_URI);
		google.setUserInfoUri(USER_INFO_URI);

		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new GoogleOAuthProviderClient(builder.build(), properties, decoderProvider, valueGenerator);
	}

	@Test
	@DisplayName("state·PKCE·nonce를 포함한 Google 인증 URL을 생성한다")
	void buildsAuthorizationUriWithStatePkceAndNonce() {
		String uri = client.createAuthorizationUri("state", "challenge", "nonce");

		assertThat(uri).contains("client_id=google-client");
		assertThat(uri).contains("redirect_uri=http://localhost:8080/api/auth/google/callback");
		assertThat(uri).contains("state=state");
		assertThat(uri).contains("code_challenge=challenge");
		assertThat(uri).contains("code_challenge_method=S256");
		assertThat(uri).contains("nonce=nonce");
	}

	@Test
	@DisplayName("token과 사용자 정보를 조회하고 검증된 Google 프로필을 매핑한다")
	void exchangesCodeValidatesIdTokenAndMapsVerifiedProfile() {
		Jwt idToken = Jwt.withTokenValue("id-token")
			.header("alg", "RS256")
			.subject("google-user")
			.audience(List.of(CLIENT_ID))
			.claim("nonce", "nonce")
			.build();
		JwtDecoder decoder = idTokenDecoder(idToken);
		decoderProvider.set(decoder);

		server.expect(requestTo(TOKEN_URI))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess("{\"access_token\":\"access-token\",\"id_token\":\"id-token\"}",
				MediaType.APPLICATION_JSON));
		server.expect(requestTo(USER_INFO_URI))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer access-token"))
			.andRespond(withSuccess(
				"{\"sub\":\"google-user\",\"name\":\"Google User\",\"email\":\"user@example.com\",\"email_verified\":true}",
				MediaType.APPLICATION_JSON));

		var profile = client.authenticateUser("authorization-code", "verifier", valueGenerator.hashWithSha256("nonce"));

		assertThat(profile.getProvider()).isEqualTo(SocialProvider.GOOGLE);
		assertThat(profile.getProviderUserId()).isEqualTo("google-user");
		assertThat(profile.getDisplayName()).isEqualTo("Google User");
		assertThat(profile.getVerifiedEmail()).isEqualTo("user@example.com");
		server.verify();
	}

	@Test
	@DisplayName("client audience가 다른 ID token을 거부한다")
	void rejectsIdTokenWithWrongAudience() {
		Jwt idToken = Jwt.withTokenValue("id-token")
			.header("alg", "RS256")
			.subject("google-user")
			.audience(List.of("another-client"))
			.claim("nonce", "nonce")
			.build();
		JwtDecoder decoder = idTokenDecoder(idToken);
		decoderProvider.set(decoder);
		server.expect(requestTo(TOKEN_URI)).andRespond(withSuccess(
			"{\"access_token\":\"access-token\",\"id_token\":\"id-token\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.authenticateUser("code", "verifier", valueGenerator.hashWithSha256("nonce")))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
	}

	@Test
	@DisplayName("nonce가 다른 ID token을 거부한다")
	void rejectsIdTokenWithWrongNonce() {
		Jwt idToken = Jwt.withTokenValue("id-token")
			.header("alg", "RS256")
			.subject("google-user")
			.audience(List.of(CLIENT_ID))
			.claim("nonce", "another-nonce")
			.build();
		JwtDecoder decoder = idTokenDecoder(idToken);
		decoderProvider.set(decoder);
		server.expect(requestTo(TOKEN_URI)).andRespond(withSuccess(
			"{\"access_token\":\"access-token\",\"id_token\":\"id-token\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.authenticateUser("code", "verifier", valueGenerator.hashWithSha256("nonce")))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
	}

	@Test
	@DisplayName("ID token과 사용자 정보의 subject가 다르면 거부한다")
	void rejectsUserInfoWhenSubjectDoesNotMatchTheIdToken() {
		Jwt idToken = Jwt.withTokenValue("id-token")
			.header("alg", "RS256")
			.subject("google-user")
			.audience(List.of(CLIENT_ID))
			.claim("nonce", "nonce")
			.build();
		JwtDecoder decoder = idTokenDecoder(idToken);
		decoderProvider.set(decoder);
		server.expect(requestTo(TOKEN_URI)).andRespond(withSuccess(
			"{\"access_token\":\"access-token\",\"id_token\":\"id-token\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(USER_INFO_URI)).andRespond(withSuccess(
			"{\"sub\":\"another-google-user\",\"email_verified\":true}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.authenticateUser("code", "verifier", valueGenerator.hashWithSha256("nonce")))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
	}

	@Test
	@DisplayName("Google 전송 오류를 외부 서비스 오류로 변환한다")
	void convertsTransportFailureToExternalServiceError() {
		server.expect(requestTo(TOKEN_URI)).andRespond(withException(new IOException("connection failed")));

		assertThatThrownBy(() -> client.authenticateUser("code", "verifier", "nonce-hash"))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
	}

	private JwtDecoder idTokenDecoder(Jwt idToken) {
		JwtDecoder decoder = mock(JwtDecoder.class);
		given(decoder.decode("id-token")).willReturn(idToken);
		return decoder;
	}
}
