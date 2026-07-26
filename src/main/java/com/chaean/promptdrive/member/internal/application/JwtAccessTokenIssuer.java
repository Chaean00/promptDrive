package com.chaean.promptdrive.member.internal.application;

import java.time.Instant;
import java.util.List;

import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.member.internal.persistence.Member;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtAccessTokenIssuer {

	private final ObjectProvider<JwtEncoder> jwtEncoderProvider;
	private final MemberOAuthProperties properties;

	public JwtAccessTokenIssuer(ObjectProvider<JwtEncoder> jwtEncoderProvider, MemberOAuthProperties properties) {
		this.jwtEncoderProvider = jwtEncoderProvider;
		this.properties = properties;
	}

	public String issue(Member member, Instant now) {
		JwtEncoder jwtEncoder = jwtEncoderProvider.getIfAvailable();
		if (jwtEncoder == null || isBlank(properties.getJwt().getIssuer()) || isBlank(properties.getJwt().getAudience())) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "JWT signing is not configured");
		}
		Instant expiresAt = now.plus(properties.getJwt().getAccessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getJwt().getIssuer())
				.audience(List.of(properties.getJwt().getAudience()))
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
