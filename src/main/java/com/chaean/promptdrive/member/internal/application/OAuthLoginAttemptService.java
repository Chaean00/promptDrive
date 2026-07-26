package com.chaean.promptdrive.member.internal.application;

import java.time.Instant;

import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttempt;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttemptRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuthLoginAttemptService {

	private final OAuthLoginAttemptRepository loginAttemptRepository;

	public OAuthLoginAttemptService(OAuthLoginAttemptRepository loginAttemptRepository) {
		this.loginAttemptRepository = loginAttemptRepository;
	}

	@Transactional
	public OAuthLoginAttempt consume(SocialProvider provider, String stateHash) {
		OAuthLoginAttempt attempt = loginAttemptRepository.findByStateHash(stateHash).orElseThrow(this::invalidLogin);
		if (attempt.getProvider() != provider || !attempt.consume(Instant.now())) {
			throw invalidLogin();
		}
		return attempt;
	}

	private ResponseStatusException invalidLogin() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth login attempt");
	}
}
