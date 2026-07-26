package com.chaean.promptdrive.common.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AccessTokenTypeValidator implements OAuth2TokenValidator<Jwt> {

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		return "access".equals(jwt.getClaimAsString("token_type"))
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(
						new OAuth2Error("INVALID_TOKEN_TYPE", "접근 토큰 유형이 올바르지 않습니다.", null));
	}
}
