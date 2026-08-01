package com.chaean.promptdrive.member.internal.application;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
public class OAuthAuthenticationService {

	private static final Duration LOGIN_ATTEMPT_TTL = Duration.ofMinutes(5);

	private final Map<SocialProvider, OAuthProviderClient> providerClients;
	private final OAuthLoginAttemptRepository loginAttemptRepository;
	private final ConsumeOAuthLoginAttemptService consumeOAuthLoginAttemptService;
	private final SocialLoginMemberCommandService socialLoginMemberCommandService;
	private final PkceStateCipher pkceStateCipher;
	private final RefreshTokenManagementService refreshTokenManagementService;
	private final MemberOAuthProperties properties;
	private final OAuthSecurityValueGenerator valueGenerator;

	public OAuthAuthenticationService(List<OAuthProviderClient> providerClients, OAuthLoginAttemptRepository loginAttemptRepository,
			ConsumeOAuthLoginAttemptService consumeOAuthLoginAttemptService, SocialLoginMemberCommandService socialLoginMemberCommandService, PkceStateCipher pkceStateCipher,
			RefreshTokenManagementService refreshTokenManagementService, MemberOAuthProperties properties, OAuthSecurityValueGenerator valueGenerator) {
		this.providerClients = new EnumMap<>(SocialProvider.class);
		providerClients.forEach(client -> this.providerClients.put(client.provider(), client));
		this.loginAttemptRepository = loginAttemptRepository;
		this.consumeOAuthLoginAttemptService = consumeOAuthLoginAttemptService;
		this.socialLoginMemberCommandService = socialLoginMemberCommandService;
		this.pkceStateCipher = pkceStateCipher;
		this.refreshTokenManagementService = refreshTokenManagementService;
		this.properties = properties;
		this.valueGenerator = valueGenerator;
	}

	@Transactional
	public OAuthLoginStartResponse startOAuthLogin(SocialProvider provider, String requestedReturnPath) {
		return startOAuthLogin(provider, requestedReturnPath, null);
	}

	@Transactional
	public OAuthLoginStartResponse startOAuthLogin(SocialProvider provider, String requestedReturnPath, String frontendOrigin) {
		OAuthProviderClient client = requireProviderClient(provider);
		String returnPath = resolveAllowedReturnPath(requestedReturnPath);
		loginAttemptRepository.deleteByExpiresAtBefore(Instant.now());

		String state = valueGenerator.generateSecureValue();
		if (frontendOrigin != null && !frontendOrigin.isBlank()) {
			state = state + "." + Base64.getUrlEncoder().withoutPadding()
					.encodeToString(frontendOrigin.getBytes(StandardCharsets.UTF_8));
		}
		String verifier = valueGenerator.generateSecureValue();
		String nonce = valueGenerator.generateSecureValue();
		loginAttemptRepository.save(new OAuthLoginAttempt(provider, valueGenerator.hashWithSha256(state), pkceStateCipher.encryptPkceVerifier(verifier),
				valueGenerator.hashWithSha256(nonce), returnPath, Instant.now().plus(LOGIN_ATTEMPT_TTL)));

		return OAuthLoginStartResponse.of(client.createAuthorizationUri(state, valueGenerator.createPkceCodeChallenge(verifier), nonce), state);
	}

	public OAuthLoginResponse completeOAuthLogin(SocialProvider provider, String authorizationCode, String state) {
		if (authorizationCode == null || authorizationCode.isBlank() || state == null || state.isBlank()) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
		}
		OAuthLoginAttempt attempt = consumeOAuthLoginAttemptService.consumeOAuthLoginAttempt(provider, valueGenerator.hashWithSha256(state));
		String verifier = pkceStateCipher.decryptPkceVerifier(attempt.getEncryptedPkceVerifier());
		SocialIdentityProfileResponse profile = requireProviderClient(provider).authenticateUser(authorizationCode, verifier, attempt.getNonceHash());
		Member member = socialLoginMemberCommandService.getOrCreateSocialLoginMember(profile);

		return OAuthLoginResponse.of(refreshTokenManagementService.issueRefreshToken(member), attempt.getReturnPath());
	}

	public void consumeOAuthLoginAttempt(SocialProvider provider, String state) {
		consumeOAuthLoginAttemptService.consumeOAuthLoginAttempt(provider, valueGenerator.hashWithSha256(state));
	}

	private OAuthProviderClient requireProviderClient(SocialProvider provider) {
		OAuthProviderClient client = providerClients.get(provider);
		if (client == null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return client;
	}

	private String resolveAllowedReturnPath(String requestedReturnPath) {
		String returnPath = requestedReturnPath == null || requestedReturnPath.isBlank() ? "/" : requestedReturnPath;
		if (!returnPath.startsWith("/") || returnPath.startsWith("//") || !properties.getAllowedReturnPaths().contains(returnPath)) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return returnPath;
	}
}
