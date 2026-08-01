package com.chaean.promptdrive.member.internal.persistence;

import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface OAuthLoginAttemptRepository extends JpaRepository<OAuthLoginAttempt, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<OAuthLoginAttempt> findByStateHash(String stateHash);

	long deleteByExpiresAtBefore(Instant expiresAt);
}
