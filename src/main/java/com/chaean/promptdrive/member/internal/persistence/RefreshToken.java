package com.chaean.promptdrive.member.internal.persistence;

import java.time.Instant;
import java.sql.Types;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;

import com.chaean.promptdrive.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_token", indexes = {
		@Index(name = "idx_refresh_token_member_id", columnList = "member_id"),
		@Index(name = "idx_refresh_token_family_id", columnList = "family_id")
})
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Member member;

	@Column(name = "family_id", nullable = false, length = 36)
	@JdbcTypeCode(Types.CHAR)
	private UUID familyId;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "predecessor_id")
	private Long predecessorId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "reused_at")
	private Instant reusedAt;

	private RefreshToken(Member member, UUID familyId, String tokenHash, Long predecessorId, Instant expiresAt) {
		this.member = member;
		this.familyId = familyId;
		this.tokenHash = tokenHash;
		this.predecessorId = predecessorId;
		this.expiresAt = expiresAt;
	}

	public static RefreshToken issue(Member member, UUID familyId, String tokenHash, Instant expiresAt) {
		return new RefreshToken(member, familyId, tokenHash, null, expiresAt);
	}

	public static RefreshToken rotate(Member member, UUID familyId, String tokenHash, Long predecessorId,
			Instant expiresAt) {
		return new RefreshToken(member, familyId, tokenHash, predecessorId, expiresAt);
	}

	public boolean isActive(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revokeRefreshToken(Instant now) {
		revokedAt = now;
	}

	public void markReused(Instant now) {
		reusedAt = now;
	}
}
