package com.chaean.promptdrive.member.internal.web;

import java.time.Duration;

import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.application.OAuthLoginService;
import com.chaean.promptdrive.member.internal.application.RefreshTokenService;
import com.chaean.promptdrive.member.internal.application.TokenPair;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class OAuthAuthController {

	private static final String REFRESH_COOKIE_NAME = "refresh_token";
	private static final String REFRESH_COOKIE_PATH = "/api/auth/refresh";
	private static final String LOGIN_STATE_COOKIE_NAME = "oauth_login_state";

	private final OAuthLoginService oauthLoginService;
	private final RefreshTokenService refreshTokenService;
	private final OAuthOriginValidator originValidator;
	private final MemberOAuthProperties properties;

	public OAuthAuthController(OAuthLoginService oauthLoginService, RefreshTokenService refreshTokenService,
			OAuthOriginValidator originValidator, MemberOAuthProperties properties) {
		this.oauthLoginService = oauthLoginService;
		this.refreshTokenService = refreshTokenService;
		this.originValidator = originValidator;
		this.properties = properties;
	}

	@GetMapping("/{provider}/start")
	public ResponseEntity<Void> start(@PathVariable String provider,
			@RequestParam(required = false) String returnPath) {
		OAuthLoginService.LoginStart loginStart = oauthLoginService.start(parseProvider(provider), returnPath);
		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, loginStart.getAuthorizationUri())
				.header(HttpHeaders.SET_COOKIE, loginStateCookie(loginStart.getState()).toString()).build();
	}

	@GetMapping("/csrf")
	public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
		return ResponseEntity.noContent().header(csrfToken.getHeaderName(), csrfToken.getToken()).build();
	}

	@GetMapping("/{provider}/callback")
	public ResponseEntity<AuthTokenResponse> callback(@PathVariable String provider,
			@RequestParam(required = false) String code, @RequestParam(required = false) String state,
			@RequestParam(required = false) String error,
			@CookieValue(name = LOGIN_STATE_COOKIE_NAME, required = false) String browserState) {
		SocialProvider socialProvider = parseProvider(provider);
		if (state == null || !state.equals(browserState)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth login attempt");
		}
		if (error != null) {
			oauthLoginService.consume(socialProvider, state);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OAuth authorization was denied");
		}
		OAuthLoginService.LoginResult result = oauthLoginService.callback(socialProvider, code, state);
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(result.getTokens()).toString())
				.header(HttpHeaders.SET_COOKIE, deleteLoginStateCookie().toString())
				.body(AuthTokenResponse.of(result.getTokens().getAccessToken(), result.getReturnPath()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthTokenResponse> refresh(
			@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
			HttpServletRequest request) {
		originValidator.requireAllowedOrigin(request);
		try {
			TokenPair tokenPair = refreshTokenService.rotate(requireRefreshToken(refreshToken));
			if (tokenPair == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
			}
			return tokenResponse(tokenPair, "/");
		} catch (ResponseStatusException exception) {
			if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
			}
			throw exception;
		}
	}

	@PostMapping("/refresh/logout")
	public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
			HttpServletRequest request) {
		originValidator.requireAllowedOrigin(request);
		refreshTokenService.revoke(refreshToken);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
	}

	private ResponseEntity<AuthTokenResponse> tokenResponse(TokenPair tokenPair, String returnPath) {
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(tokenPair).toString())
				.body(AuthTokenResponse.of(tokenPair.getAccessToken(), returnPath));
	}

	private ResponseCookie refreshCookie(TokenPair tokenPair) {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, tokenPair.getRefreshToken())
				.httpOnly(true).secure(properties.getJwt().isRefreshCookieSecure()).sameSite("Lax")
				.path(REFRESH_COOKIE_PATH).maxAge(tokenPair.getRefreshTokenTtl()).build();
	}

	private ResponseCookie deleteRefreshCookie() {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, "").httpOnly(true).secure(properties.getJwt().isRefreshCookieSecure())
				.sameSite("Lax").path(REFRESH_COOKIE_PATH).maxAge(Duration.ZERO).build();
	}

	private ResponseCookie loginStateCookie(String state) {
		return ResponseCookie.from(LOGIN_STATE_COOKIE_NAME, state).httpOnly(true)
				.secure(properties.getJwt().isRefreshCookieSecure()).sameSite("Lax").path("/api/auth")
				.maxAge(Duration.ofMinutes(5)).build();
	}

	private ResponseCookie deleteLoginStateCookie() {
		return ResponseCookie.from(LOGIN_STATE_COOKIE_NAME, "").httpOnly(true)
				.secure(properties.getJwt().isRefreshCookieSecure()).sameSite("Lax").path("/api/auth")
				.maxAge(Duration.ZERO).build();
	}

	private SocialProvider parseProvider(String provider) {
		try {
			return SocialProvider.from(provider);
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unsupported OAuth provider");
		}
	}

	private String requireRefreshToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}
		return refreshToken;
	}
}
