package com.chaean.promptdrive.member.internal.application;

import java.time.Instant;
import java.util.List;

import com.chaean.promptdrive.common.config.JwtProperties;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.member.internal.persistence.Member;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAccessTokenIssuer {

	private final ObjectProvider<JwtEncoder> jwtEncoderProvider;
	private final JwtProperties properties;

	public String issue(Member member, Instant now) {
		JwtEncoder jwtEncoder = jwtEncoderProvider.getIfAvailable();
		if (jwtEncoder == null || isBlank(properties.getIssuer()) || isBlank(properties.getAudience())) {
			throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
		}

		Instant expiresAt = now.plus(properties.getAccessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getIssuer())
				.audience(List.of(properties.getAudience()))
				.subject(member.getId().toString())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("member_id", member.getId().toString())
				.claim("roles", List.of(member.getRole().getCode()))
				.claim("token_type", "access")
				.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
