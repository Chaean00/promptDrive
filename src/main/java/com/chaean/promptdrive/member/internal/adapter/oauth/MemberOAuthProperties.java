package com.chaean.promptdrive.member.internal.adapter.oauth;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "member.oauth")
public class MemberOAuthProperties {

	private Provider google = new Provider(
			"https://accounts.google.com/o/oauth2/v2/auth",
			"https://oauth2.googleapis.com/token",
			"https://openidconnect.googleapis.com/v1/userinfo");
	private Provider kakao = new Provider(
			"https://kauth.kakao.com/oauth/authorize",
			"https://kauth.kakao.com/oauth/token",
			"https://kapi.kakao.com/v2/user/me");
	private List<String> allowedReturnPaths = List.of("/");
	private List<String> allowedOrigins = List.of();
	private String stateEncryptionKey;
	private Jwt jwt = new Jwt();

	public void requireConfigured(Provider provider) {
		if (isBlank(provider.getClientId()) || isBlank(provider.getClientSecret()) || isBlank(provider.getRedirectUri())) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OAuth provider is not configured");
		}
	}

	public void requireStateEncryptionKey() {
		if (isBlank(stateEncryptionKey)) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OAuth state encryption is not configured");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	@Getter
	@Setter
	public static class Provider {
		private String clientId;
		private String clientSecret;
		private String redirectUri;
		private String authorizationUri;
		private String tokenUri;
		private String userInfoUri;

		public Provider() {
		}

		public Provider(String authorizationUri, String tokenUri, String userInfoUri) {
			this.authorizationUri = authorizationUri;
			this.tokenUri = tokenUri;
			this.userInfoUri = userInfoUri;
		}
	}

	@Getter
	@Setter
	public static class Jwt {
		private String issuer;
		private String audience;
		private String signingKey;
		private Duration accessTokenTtl = Duration.ofMinutes(15);
		private Duration refreshTokenTtl = Duration.ofDays(30);
		private boolean refreshCookieSecure = true;
	}
}
