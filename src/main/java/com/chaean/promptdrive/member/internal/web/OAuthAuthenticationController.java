package com.chaean.promptdrive.member.internal.web;

import java.time.Duration;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.chaean.promptdrive.common.config.JwtProperties;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.member.internal.application.OAuthAuthenticationService;
import com.chaean.promptdrive.member.internal.application.RefreshTokenManagementService;
import com.chaean.promptdrive.member.internal.dto.AuthTokenResponse;
import com.chaean.promptdrive.member.internal.dto.OAuthLoginResponse;
import com.chaean.promptdrive.member.internal.dto.OAuthLoginStartResponse;
import com.chaean.promptdrive.member.internal.dto.TokenPairResponse;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.web.validator.OAuthOriginValidator;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthAuthenticationController {

	private static final String REFRESH_COOKIE_NAME = "refresh_token";
	private static final String REFRESH_COOKIE_PATH = "/api/auth/refresh";
	private static final String LOGIN_STATE_COOKIE_NAME = "oauth_login_state";
	private static final String LOGIN_STATE_DELIMITER = "~";
	private static final String FRONTEND_ORIGIN_COOKIE_NAME = "oauth_frontend_origin";

	private final OAuthAuthenticationService oauthAuthenticationService;
	private final RefreshTokenManagementService refreshTokenManagementService;
	private final OAuthOriginValidator originValidator;
	private final JwtProperties jwtProperties;
	private final MemberOAuthProperties oauthProperties;

	@GetMapping("/{provider}/start")
	public ResponseEntity<Void> startOAuthLogin(
			@PathVariable String provider,
			@RequestParam(required = false) String returnPath,
			@RequestParam(required = false) String frontendOrigin,
			@CookieValue(name = LOGIN_STATE_COOKIE_NAME, required = false) String browserStates
	) {
		String resolvedFrontendOrigin = resolveFrontendOrigin(frontendOrigin);
		OAuthLoginStartResponse loginStart = oauthAuthenticationService.startOAuthLogin(parseSocialProvider(provider), returnPath, resolvedFrontendOrigin);
		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, loginStart.getAuthorizationUri())
					.header(HttpHeaders.SET_COOKIE, loginStateCookie(appendLoginState(browserStates, loginStart.getState())).toString())
				.header(HttpHeaders.SET_COOKIE, frontendOriginCookie(resolvedFrontendOrigin).toString()).build();
	}

	@GetMapping("/csrf")
	public ResponseEntity<Void> issueCsrfToken(CsrfToken csrfToken) {
		return ResponseEntity.noContent().header(csrfToken.getHeaderName(), csrfToken.getToken()).build();
	}

	@GetMapping("/{provider}/callback")
	public ResponseEntity<Void> completeOAuthLogin(
			@PathVariable String provider,
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String error,
			@CookieValue(name = LOGIN_STATE_COOKIE_NAME, required = false) String browserState,
			@CookieValue(name = FRONTEND_ORIGIN_COOKIE_NAME, required = false) String browserFrontendOrigin
	) {
		SocialProvider socialProvider = parseSocialProvider(provider);
		if (state == null || !containsLoginState(browserState, state)) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
		}
		if (error != null) {
			oauthAuthenticationService.consumeOAuthLoginAttempt(socialProvider, state);
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
		}
		OAuthLoginResponse result = oauthAuthenticationService.completeOAuthLogin(socialProvider, code, state);
		AuthTokenResponse tokenResponse = AuthTokenResponse.of(result.getTokens().getAccessToken(), result.getReturnPath());
		String frontendOrigin = resolveFrontendOrigin(extractFrontendOrigin(state));
		if (frontendOrigin.equals(oauthProperties.getFrontendOrigin()) && browserFrontendOrigin != null) {
			frontendOrigin = resolveFrontendOrigin(browserFrontendOrigin);
		}
		URI frontendRedirect = frontendRedirectUri(frontendOrigin, tokenResponse.getReturnPath(), tokenResponse.getAccessToken());
		return ResponseEntity.status(HttpStatus.FOUND).location(frontendRedirect)
				.header(HttpHeaders.SET_COOKIE, refreshCookie(result.getTokens()).toString())
					.header(HttpHeaders.SET_COOKIE, loginStateCookie(removeLoginState(browserState, state)).toString())
				.header(HttpHeaders.SET_COOKIE, deleteFrontendOriginCookie().toString())
				.build();
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthTokenResponse> refreshAccessToken(
			@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
			HttpServletRequest request
	) {
		originValidator.requireAllowedOrigin(request);
		if (refreshToken == null || refreshToken.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
		}
		TokenPairResponse tokenPair = refreshTokenManagementService.rotateRefreshToken(refreshToken);
		if (tokenPair == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
		}
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(tokenPair).toString())
				.body(AuthTokenResponse.of(tokenPair.getAccessToken(), "/"));
	}

	@PostMapping("/refresh/logout")
	public ResponseEntity<Void> logoutMember(
			@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
			HttpServletRequest request
	) {
		originValidator.requireAllowedOrigin(request);
		refreshTokenManagementService.revokeRefreshToken(refreshToken);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
	}

	private URI frontendRedirectUri(String frontendOrigin, String returnPath, String accessToken) {
		return UriComponentsBuilder.fromUriString(frontendOrigin)
				.path(returnPath)
				.fragment("access_token=" + accessToken)
				.build()
				.encode()
				.toUri();
	}

	private String extractFrontendOrigin(String state) {
		if (state == null) {
			return null;
		}
		int separator = state.indexOf('.');
		if (separator < 0 || separator == state.length() - 1) {
			return null;
		}
		try {
			return new String(Base64.getUrlDecoder().decode(state.substring(separator + 1)), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private String resolveFrontendOrigin(String requestedOrigin) {
		if (requestedOrigin != null && oauthProperties.getAllowedOrigins().contains(requestedOrigin)) {
			return requestedOrigin;
		}
		return oauthProperties.getFrontendOrigin();
	}

	private ResponseCookie frontendOriginCookie(String frontendOrigin) {
		return ResponseCookie.from(FRONTEND_ORIGIN_COOKIE_NAME, frontendOrigin).httpOnly(true)
				.secure(jwtProperties.isRefreshCookieSecure()).sameSite("Lax").path("/api/auth")
				.maxAge(Duration.ofMinutes(5)).build();
	}

	private ResponseCookie deleteFrontendOriginCookie() {
		return ResponseCookie.from(FRONTEND_ORIGIN_COOKIE_NAME, "").httpOnly(true)
				.secure(jwtProperties.isRefreshCookieSecure()).sameSite("Lax").path("/api/auth")
				.maxAge(Duration.ZERO).build();
	}

	private ResponseCookie refreshCookie(TokenPairResponse tokenPair) {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, tokenPair.getRefreshToken())
				.httpOnly(true).secure(jwtProperties.isRefreshCookieSecure()).sameSite("Lax")
				.path(REFRESH_COOKIE_PATH).maxAge(tokenPair.getRefreshTokenTtl()).build();
	}

	private ResponseCookie deleteRefreshCookie() {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, "").httpOnly(true).secure(jwtProperties.isRefreshCookieSecure())
				.sameSite("Lax").path(REFRESH_COOKIE_PATH).maxAge(Duration.ZERO).build();
	}

	private ResponseCookie loginStateCookie(String state) {
		return ResponseCookie.from(LOGIN_STATE_COOKIE_NAME, state).httpOnly(true)
				.secure(jwtProperties.isRefreshCookieSecure()).sameSite("Lax").path("/api/auth")
				.maxAge(Duration.ofMinutes(5)).build();
	}

	private boolean containsLoginState(String browserStates, String state) {
		return browserStates != null && splitLoginStates(browserStates).anyMatch(state::equals);
	}

	private String appendLoginState(String browserStates, String state) {
		String[] existingStates = browserStates == null ? new String[0]
			: splitLoginStates(browserStates).filter(value -> !value.isBlank()).toArray(String[]::new);
		int firstStateIndex = Math.max(0, existingStates.length - 4);
		return Stream.concat(Arrays.stream(existingStates, firstStateIndex, existingStates.length), Stream.of(state))
			.collect(Collectors.joining(LOGIN_STATE_DELIMITER));
	}

	private String removeLoginState(String browserStates, String state) {
		return splitLoginStates(browserStates).filter(value -> !value.equals(state) && !value.isBlank())
			.collect(Collectors.joining(LOGIN_STATE_DELIMITER));
	}

	private Stream<String> splitLoginStates(String browserStates) {
		return Arrays.stream(browserStates.split("[~,]"));
	}

	private SocialProvider parseSocialProvider(String provider) {
		return SocialProvider.from(provider)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST));
	}

}
