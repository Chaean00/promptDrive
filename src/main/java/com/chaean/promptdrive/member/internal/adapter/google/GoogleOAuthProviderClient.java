package com.chaean.promptdrive.member.internal.adapter.google;

import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderClient;
import com.chaean.promptdrive.member.internal.application.SocialIdentityProfile;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuthProviderClient implements OAuthProviderClient {

	private final RestClient restClient;
	private final MemberOAuthProperties properties;
	private JwtDecoder idTokenDecoder;

	public GoogleOAuthProviderClient(RestClient.Builder restClientBuilder, MemberOAuthProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.GOOGLE;
	}

	@Override
	public String authorizationUri(String state, String codeChallenge, String nonce) {
		MemberOAuthProperties.Provider provider = properties.getGoogle();
		properties.requireConfigured(provider);
		return UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
				.queryParam("client_id", provider.getClientId())
				.queryParam("redirect_uri", provider.getRedirectUri())
				.queryParam("response_type", "code")
				.queryParam("scope", "openid profile email")
				.queryParam("state", state)
				.queryParam("nonce", nonce)
				.queryParam("code_challenge", codeChallenge)
				.queryParam("code_challenge_method", "S256")
				.build().encode().toUriString();
	}

	@Override
	public SocialIdentityProfile authenticate(String authorizationCode, String pkceVerifier, String nonceHash) {
		MemberOAuthProperties.Provider provider = properties.getGoogle();
		properties.requireConfigured(provider);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", authorizationCode);
		form.add("client_id", provider.getClientId());
		form.add("client_secret", provider.getClientSecret());
		form.add("redirect_uri", provider.getRedirectUri());
		form.add("grant_type", "authorization_code");
		form.add("code_verifier", pkceVerifier);
		Map<String, Object> token = requestToken(provider.getTokenUri(), form);
		String accessToken = stringValue(token, "access_token");
		String idToken = stringValue(token, "id_token");
		if (accessToken == null || idToken == null) {
			throw providerFailure();
		}
		Jwt validatedIdToken = validateIdToken(idToken, provider.getClientId(), nonceHash);
		Map<String, Object> userInfo = restClient.get().uri(provider.getUserInfoUri())
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve().onStatus(status -> status.isError(), (request, response) -> { throw providerFailure(); })
				.body(Map.class);
		String subject = stringValue(userInfo, "sub");
		if (subject == null || subject.isBlank() || !subject.equals(validatedIdToken.getSubject())) {
			throw providerFailure();
		}
		String email = booleanValue(userInfo, "email_verified") ? stringValue(userInfo, "email") : null;
		return SocialIdentityProfile.of(SocialProvider.GOOGLE, subject, stringValue(userInfo, "name"), email);
	}

	private Jwt validateIdToken(String idToken, String clientId, String nonceHash) {
		try {
			Jwt jwt = idTokenDecoder().decode(idToken);
			if (!jwt.getAudience().contains(clientId) || !nonceHash.equals(sha256(jwt.getClaimAsString("nonce")))) {
				throw providerFailure();
			}
			return jwt;
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw providerFailure();
		}
	}

	private String sha256(String value) {
		if (value == null) {
			return "";
		}
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private synchronized JwtDecoder idTokenDecoder() {
		if (idTokenDecoder == null) {
			idTokenDecoder = JwtDecoders.fromIssuerLocation("https://accounts.google.com");
		}
		return idTokenDecoder;
	}

	private Map<String, Object> requestToken(String tokenUri, MultiValueMap<String, String> form) {
		return restClient.post().uri(tokenUri).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
				.retrieve().onStatus(status -> status.isError(), (request, response) -> { throw providerFailure(); })
				.body(Map.class);
	}

	private String stringValue(Map<String, Object> values, String key) {
		Object value = values == null ? null : values.get(key);
		return value == null ? null : value.toString();
	}

	private boolean booleanValue(Map<String, Object> values, String key) {
		Object value = values == null ? null : values.get(key);
		return Boolean.TRUE.equals(value) || "true".equals(value);
	}

	private ResponseStatusException providerFailure() {
		return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google OAuth authentication failed");
	}
}
