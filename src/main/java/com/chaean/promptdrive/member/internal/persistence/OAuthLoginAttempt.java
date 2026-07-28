package com.chaean.promptdrive.member.internal.persistence;

import java.time.Instant;

import org.hibernate.annotations.SQLRestriction;

import com.chaean.promptdrive.common.persistence.BaseEntity;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "oauth_login_attempt")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthLoginAttempt extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SocialProvider provider;

	@Column(name = "state_hash", nullable = false, length = 64, unique = true)
	private String stateHash;

	@Column(name = "encrypted_pkce_verifier", nullable = false, length = 1024)
	private String encryptedPkceVerifier;

	@Column(name = "nonce_hash", length = 64)
	private String nonceHash;

	@Column(name = "return_path", nullable = false, length = 2048)
	private String returnPath;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	public OAuthLoginAttempt(SocialProvider provider, String stateHash, String encryptedPkceVerifier, String nonceHash,
			String returnPath, Instant expiresAt) {
		this.provider = provider;
		this.stateHash = stateHash;
		this.encryptedPkceVerifier = encryptedPkceVerifier;
		this.nonceHash = nonceHash;
		this.returnPath = returnPath;
		this.expiresAt = expiresAt;
	}

	public boolean consumeOAuthLoginAttempt(Instant now) {
		if (consumedAt != null || !expiresAt.isAfter(now)) {
			return false;
		}
		consumedAt = now;
		return true;
	}
}
