package com.chaean.promptdrive.member.internal.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderClient;
import com.chaean.promptdrive.member.internal.adapter.oauth.PkceStateCipher;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttempt;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttemptRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuthLoginService {

	private static final Duration LOGIN_ATTEMPT_TTL = Duration.ofMinutes(5);

	private final Map<SocialProvider, OAuthProviderClient> providerClients;
	private final OAuthLoginAttemptRepository loginAttemptRepository;
	private final OAuthLoginAttemptService loginAttemptService;
	private final SocialLoginMembershipService socialLoginMembershipService;
	private final PkceStateCipher pkceStateCipher;
	private final RefreshTokenService refreshTokenService;
	private final MemberOAuthProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public OAuthLoginService(List<OAuthProviderClient> providerClients, OAuthLoginAttemptRepository loginAttemptRepository,
			OAuthLoginAttemptService loginAttemptService,
			SocialLoginMembershipService socialLoginMembershipService, PkceStateCipher pkceStateCipher,
			RefreshTokenService refreshTokenService, MemberOAuthProperties properties) {
		this.providerClients = new EnumMap<>(SocialProvider.class);
		providerClients.forEach(client -> this.providerClients.put(client.provider(), client));
		this.loginAttemptRepository = loginAttemptRepository;
		this.loginAttemptService = loginAttemptService;
		this.socialLoginMembershipService = socialLoginMembershipService;
		this.pkceStateCipher = pkceStateCipher;
		this.refreshTokenService = refreshTokenService;
		this.properties = properties;
	}

	@Transactional
	public LoginStart start(SocialProvider provider, String requestedReturnPath) {
		OAuthProviderClient client = client(provider);
		String returnPath = allowedReturnPath(requestedReturnPath);
		loginAttemptRepository.deleteByExpiresAtBefore(Instant.now());
		String state = randomValue();
		String verifier = randomValue();
		String nonce = randomValue();
		loginAttemptRepository.save(new OAuthLoginAttempt(provider, sha256(state), pkceStateCipher.encrypt(verifier), sha256(nonce),
				returnPath, Instant.now().plus(LOGIN_ATTEMPT_TTL)));
		return LoginStart.of(client.authorizationUri(state, pkceChallenge(verifier), nonce), state);
	}

	public LoginResult callback(SocialProvider provider, String authorizationCode, String state) {
		if (authorizationCode == null || authorizationCode.isBlank() || state == null || state.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth login attempt");
		}
		OAuthLoginAttempt attempt = loginAttemptService.consume(provider, sha256(state));
		String verifier = pkceStateCipher.decrypt(attempt.getEncryptedPkceVerifier());
		SocialIdentityProfile profile = client(provider).authenticate(authorizationCode, verifier, attempt.getNonceHash());
		Member member = socialLoginMembershipService.findOrCreate(profile);
		return LoginResult.of(refreshTokenService.issue(member), attempt.getReturnPath());
	}

	public void consume(SocialProvider provider, String state) {
		loginAttemptService.consume(provider, sha256(state));
	}

	private OAuthProviderClient client(SocialProvider provider) {
		OAuthProviderClient client = providerClients.get(provider);
		if (client == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unsupported OAuth provider");
		}
		return client;
	}

	private String allowedReturnPath(String requestedReturnPath) {
		String returnPath = requestedReturnPath == null || requestedReturnPath.isBlank() ? "/" : requestedReturnPath;
		if (!returnPath.startsWith("/") || returnPath.startsWith("//") || !properties.getAllowedReturnPaths().contains(returnPath)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return path");
		}
		return returnPath;
	}

	private String randomValue() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String pkceChallenge(String verifier) {
		try {
			return Base64.getUrlEncoder().withoutPadding().encodeToString(
					MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	public static class LoginResult {
		private final TokenPair tokens;
		private final String returnPath;

		private LoginResult(TokenPair tokens, String returnPath) {
			this.tokens = tokens;
			this.returnPath = returnPath;
		}

		public static LoginResult of(TokenPair tokens, String returnPath) {
			return new LoginResult(tokens, returnPath);
		}

		public TokenPair getTokens() { return tokens; }
		public String getReturnPath() { return returnPath; }
	}

	public static class LoginStart {
		private final String authorizationUri;
		private final String state;

		private LoginStart(String authorizationUri, String state) {
			this.authorizationUri = authorizationUri;
			this.state = state;
		}

		public static LoginStart of(String authorizationUri, String state) {
			return new LoginStart(authorizationUri, state);
		}

		public String getAuthorizationUri() { return authorizationUri; }
		public String getState() { return state; }
	}
}
