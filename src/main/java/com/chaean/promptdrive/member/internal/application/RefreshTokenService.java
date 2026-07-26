package com.chaean.promptdrive.member.internal.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.persistence.Member;
import com.chaean.promptdrive.member.internal.persistence.RefreshToken;
import com.chaean.promptdrive.member.internal.persistence.RefreshTokenRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtAccessTokenIssuer jwtAccessTokenIssuer;
	private final MemberOAuthProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtAccessTokenIssuer jwtAccessTokenIssuer,
			MemberOAuthProperties properties) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtAccessTokenIssuer = jwtAccessTokenIssuer;
		this.properties = properties;
	}

	@Transactional
	public TokenPair issue(Member member) {
		Instant now = Instant.now();
		String rawToken = newToken();
		refreshTokenRepository.save(new RefreshToken(member, UUID.randomUUID(), sha256(rawToken), null,
				now.plus(properties.getJwt().getRefreshTokenTtl())));
		return TokenPair.of(jwtAccessTokenIssuer.issue(member, now), rawToken, properties.getJwt().getRefreshTokenTtl());
	}

	@Transactional
	public TokenPair rotate(String rawToken) {
		Instant now = Instant.now();
		RefreshToken current = refreshTokenRepository.findByTokenHash(sha256(rawToken)).orElse(null);
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
		refreshTokenRepository.save(new RefreshToken(current.getMember(), current.getFamilyId(), sha256(nextRawToken),
				current.getId(), now.plus(properties.getJwt().getRefreshTokenTtl())));
		return TokenPair.of(jwtAccessTokenIssuer.issue(current.getMember(), now), nextRawToken,
				properties.getJwt().getRefreshTokenTtl());
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(token -> revokeFamily(token.getFamilyId(), Instant.now()));
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

	private String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

}
