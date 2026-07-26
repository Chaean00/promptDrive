package com.chaean.promptdrive.member.internal.application;

import java.time.Instant;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.member.internal.domain.SocialProvider;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttempt;
import com.chaean.promptdrive.member.internal.persistence.OAuthLoginAttemptRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthLoginAttemptService {

	private final OAuthLoginAttemptRepository loginAttemptRepository;

	@Transactional
	public OAuthLoginAttempt consume(SocialProvider provider, String stateHash) {
		OAuthLoginAttempt attempt = loginAttemptRepository.findByStateHash(stateHash)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST));

		if (attempt.getProvider() != provider || !attempt.consume(Instant.now())) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED_REQUEST);
		}

		return attempt;
	}
}
