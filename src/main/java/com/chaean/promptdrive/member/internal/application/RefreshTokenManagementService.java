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
public class RefreshTokenManagementService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtAccessTokenIssuer jwtAccessTokenIssuer;
	private final JwtProperties properties;
	private final OAuthSecurityValueGenerator valueGenerator;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public TokenPairResponse issueRefreshToken(Member member) {
		Instant now = Instant.now();
		String rawToken = generateRefreshTokenValue();
		refreshTokenRepository.save(RefreshToken.issue(member, UUID.randomUUID(), valueGenerator.hashWithSha256(rawToken),
				now.plus(properties.getRefreshTokenTtl())));

		return TokenPairResponse.of(jwtAccessTokenIssuer.issueAccessToken(member, now), rawToken, properties.getRefreshTokenTtl());
	}

	@Transactional
	public TokenPairResponse rotateRefreshToken(String rawToken) {
		Instant now = Instant.now();
		RefreshToken current = refreshTokenRepository.findByTokenHash(valueGenerator.hashWithSha256(rawToken)).orElse(null);
		if (current == null) {
			return null;
		}

		if (!current.isActive(now)) {
			if (current.getRevokedAt() != null) {
				current.markReused(now);
			}
			revokeRefreshTokenFamily(current.getFamilyId(), now);
			return null;
		}

		current.revokeRefreshToken(now);
		String nextRawToken = generateRefreshTokenValue();
		refreshTokenRepository.save(RefreshToken.rotate(current.getMember(), current.getFamilyId(), valueGenerator.hashWithSha256(nextRawToken),
				current.getId(), now.plus(properties.getRefreshTokenTtl())));

		return TokenPairResponse.of(jwtAccessTokenIssuer.issueAccessToken(current.getMember(), now), nextRawToken,
				properties.getRefreshTokenTtl());
	}

	@Transactional
	public void revokeRefreshToken(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(valueGenerator.hashWithSha256(rawToken)).ifPresent(token -> revokeRefreshTokenFamily(token.getFamilyId(), Instant.now()));
	}

	private void revokeRefreshTokenFamily(UUID familyId, Instant now) {
		refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId)
				.forEach(token -> token.revokeRefreshToken(now));
	}

	private String generateRefreshTokenValue() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
