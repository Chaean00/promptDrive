package com.chaean.promptdrive.member.internal.application;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.adapter.oauth.OAuthProviderClient;
import com.chaean.promptdrive.member.internal.adapter.oauth.PkceStateCipher;
import com.chaean.promptdrive.member.internal.dto.OAuthLoginResponse;
import com.chaean.promptdrive.member.internal.dto.OAuthLoginStartResponse;
import com.chaean.promptdrive.member.internal.dto.SocialIdentityProfileResponse;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttempt;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttemptRepository;
import com.chaean.promptdrive.member.internal.util.OAuthSecurityValueGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final OAuthSecurityValueGenerator valueGenerator;

	public OAuthLoginService(List<OAuthProviderClient> providerClients, OAuthLoginAttemptRepository loginAttemptRepository,
			OAuthLoginAttemptService loginAttemptService, SocialLoginMembershipService socialLoginMembershipService, PkceStateCipher pkceStateCipher,
			RefreshTokenService refreshTokenService, MemberOAuthProperties properties, OAuthSecurityValueGenerator valueGenerator) {
		this.providerClients = new EnumMap<>(SocialProvider.class);
		providerClients.forEach(client -> this.providerClients.put(client.provider(), client));
		this.loginAttemptRepository = loginAttemptRepository;
		this.loginAttemptService = loginAttemptService;
		this.socialLoginMembershipService = socialLoginMembershipService;
		this.pkceStateCipher = pkceStateCipher;
		this.refreshTokenService = refreshTokenService;
		this.properties = properties;
		this.valueGenerator = valueGenerator;
	}

	@Transactional
	public OAuthLoginStartResponse start(SocialProvider provider, String requestedReturnPath) {
		OAuthProviderClient client = client(provider);
		String returnPath = allowedReturnPath(requestedReturnPath);
		loginAttemptRepository.deleteByExpiresAtBefore(Instant.now());

		String state = valueGenerator.generate();
		String verifier = valueGenerator.generate();
		String nonce = valueGenerator.generate();
		loginAttemptRepository.save(new OAuthLoginAttempt(provider, valueGenerator.sha256(state), pkceStateCipher.encrypt(verifier),
				valueGenerator.sha256(nonce), returnPath, Instant.now().plus(LOGIN_ATTEMPT_TTL)));

		return OAuthLoginStartResponse.of(client.authorizationUri(state, valueGenerator.pkceChallenge(verifier), nonce), state);
	}

	public OAuthLoginResponse callback(SocialProvider provider, String authorizationCode, String state) {
		if (authorizationCode == null || authorizationCode.isBlank() || state == null || state.isBlank()) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
		}
		OAuthLoginAttempt attempt = loginAttemptService.consume(provider, valueGenerator.sha256(state));
		String verifier = pkceStateCipher.decrypt(attempt.getEncryptedPkceVerifier());
		SocialIdentityProfileResponse profile = client(provider).authenticate(authorizationCode, verifier, attempt.getNonceHash());
		Member member = socialLoginMembershipService.findOrCreate(profile);

		return OAuthLoginResponse.of(refreshTokenService.issue(member), attempt.getReturnPath());
	}

	public void consume(SocialProvider provider, String state) {
		loginAttemptService.consume(provider, valueGenerator.sha256(state));
	}

	private OAuthProviderClient client(SocialProvider provider) {
		OAuthProviderClient client = providerClients.get(provider);
		if (client == null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return client;
	}

	private String allowedReturnPath(String requestedReturnPath) {
		String returnPath = requestedReturnPath == null || requestedReturnPath.isBlank() ? "/" : requestedReturnPath;
		if (!returnPath.startsWith("/") || returnPath.startsWith("//") || !properties.getAllowedReturnPaths().contains(returnPath)) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return returnPath;
	}
}
