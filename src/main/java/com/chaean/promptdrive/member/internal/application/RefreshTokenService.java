package com.chaean.promptdrive.member.internal.application;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.chaean.promptdrive.common.config.JwtProperties;
import com.chaean.promptdrive.member.internal.dto.TokenPairResponse;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.RefreshToken;
import com.chaean.promptdrive.member.internal.persistence.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaean.promptdrive.member.internal.util.OAuthSecurityValueGenerator;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtAccessTokenIssuer jwtAccessTokenIssuer;
	private final JwtProperties properties;
	private final OAuthSecurityValueGenerator valueGenerator;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public TokenPairResponse issue(Member member) {
		Instant now = Instant.now();
		String rawToken = newToken();
		refreshTokenRepository.save(new RefreshToken(member, UUID.randomUUID(), valueGenerator.sha256(rawToken), null,
				now.plus(properties.getRefreshTokenTtl())));

		return TokenPairResponse.of(jwtAccessTokenIssuer.issue(member, now), rawToken, properties.getRefreshTokenTtl());
	}

	@Transactional
	public TokenPairResponse rotate(String rawToken) {
		Instant now = Instant.now();
		RefreshToken current = refreshTokenRepository.findByTokenHash(valueGenerator.sha256(rawToken)).orElse(null);
		if (current == null) {
			return null;
		}

		if (!current.isActive(now)) {
			if (current.getRevokedAt() != null) {
				current.markReused(now);
			}
			revokeFamily(current.getFamilyId(), now);
			return null;
		}

		current.revoke(now);
		String nextRawToken = newToken();
		refreshTokenRepository.save(new RefreshToken(current.getMember(), current.getFamilyId(), valueGenerator.sha256(nextRawToken),
				current.getId(), now.plus(properties.getRefreshTokenTtl())));

		return TokenPairResponse.of(jwtAccessTokenIssuer.issue(current.getMember(), now), nextRawToken,
				properties.getRefreshTokenTtl());
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(valueGenerator.sha256(rawToken)).ifPresent(token -> revokeFamily(token.getFamilyId(), Instant.now()));
	}

	private void revokeFamily(UUID familyId, Instant now) {
		refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId)
				.forEach(token -> token.revoke(now));
	}

	private String newToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
