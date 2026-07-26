package com.chaean.promptdrive.common.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

	private final String expectedAudience;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		return jwt.getAudience().contains(expectedAudience)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(
						new OAuth2Error("INVALID_TOKEN_AUDIENCE", "토큰 대상이 올바르지 않습니다.", null));
	}
}
