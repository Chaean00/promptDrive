package com.chaean.promptdrive.member.internal.adapter.kakao;

import java.util.Map;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderClient;
import com.chaean.promptdrive.member.internal.application.SocialIdentityProfile;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoOAuthProviderClient implements OAuthProviderClient {

	private final RestClient restClient;
	private final MemberOAuthProperties properties;

	public KakaoOAuthProviderClient(RestClient.Builder restClientBuilder, MemberOAuthProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.KAKAO;
	}

	@Override
	public String authorizationUri(String state, String codeChallenge, String nonce) {
		MemberOAuthProperties.Provider provider = properties.getKakao();
		properties.requireConfigured(provider);
		return UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
				.queryParam("client_id", provider.getClientId())
				.queryParam("redirect_uri", provider.getRedirectUri())
				.queryParam("response_type", "code")
				.queryParam("scope", "profile_nickname account_email")
				.queryParam("state", state)
				.queryParam("code_challenge", codeChallenge)
				.queryParam("code_challenge_method", "S256")
				.build().encode().toUriString();
	}

	@Override
	public SocialIdentityProfile authenticate(String authorizationCode, String pkceVerifier, String nonce) {
		MemberOAuthProperties.Provider provider = properties.getKakao();
		properties.requireConfigured(provider);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", authorizationCode);
		form.add("client_id", provider.getClientId());
		form.add("client_secret", provider.getClientSecret());
		form.add("redirect_uri", provider.getRedirectUri());
		form.add("grant_type", "authorization_code");
		form.add("code_verifier", pkceVerifier);
		Map<String, Object> token = restClient.post().uri(provider.getTokenUri()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form).retrieve().onStatus(status -> status.isError(), (request, response) -> { throw providerFailure(); })
				.body(Map.class);
		String accessToken = stringValue(token, "access_token");
		if (accessToken == null) {
			throw providerFailure();
		}
		Map<String, Object> userInfo = restClient.get().uri(provider.getUserInfoUri())
				.headers(headers -> headers.setBearerAuth(accessToken)).retrieve()
				.onStatus(status -> status.isError(), (request, response) -> { throw providerFailure(); }).body(Map.class);
		String id = stringValue(userInfo, "id");
		if (id == null || id.isBlank()) {
			throw providerFailure();
		}
		Map<String, Object> account = nestedMap(userInfo, "kakao_account");
		String nickname = stringValue(nestedMap(account, "profile"), "nickname");
		String email = booleanValue(account, "is_email_valid") && booleanValue(account, "is_email_verified")
				? stringValue(account, "email") : null;
		return SocialIdentityProfile.of(SocialProvider.KAKAO, id, nickname, email);
	}

	private String stringValue(Map<String, Object> values, String key) {
		Object value = values == null ? null : values.get(key);
		return value == null ? null : value.toString();
	}

	private boolean booleanValue(Map<String, Object> values, String key) {
		Object value = values == null ? null : values.get(key);
		return Boolean.TRUE.equals(value) || "true".equals(value);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> nestedMap(Map<String, Object> values, String key) {
		Object value = values == null ? null : values.get(key);
		return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
	}

	private ResponseStatusException providerFailure() {
		return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao OAuth authentication failed");
	}
}
