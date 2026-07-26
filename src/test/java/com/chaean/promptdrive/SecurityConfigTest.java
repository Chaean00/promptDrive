package com.chaean.promptdrive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import com.chaean.promptdrive.common.config.SecurityConfig;

class SecurityConfigTest {

	private static final String SIGNING_KEY = Base64.getEncoder().encodeToString(new byte[32]);


	@Test
	void mapsValidatedJwtClaimsToPrincipalAndAuthorities() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("member-1")
				.claim("member_id", "member-1")
				.claim("roles", List.of("MEMBER", "ROLE_ADMIN"))
				.issuedAt(Instant.EPOCH)
				.expiresAt(Instant.MAX)
				.build();

		Authentication authentication = new SecurityConfig()
				.jwtAuthenticationConverter()
				.convert(jwt);

		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("member-1");
		assertThat(authentication.getAuthorities())
				.extracting(authority -> authority.getAuthority())
				.contains("ROLE_MEMBER", "ROLE_ADMIN", "FACTOR_BEARER");
	}

	@Test
	void acceptsOnlySignedAccessTokensForConfiguredIssuerAndAudience() {
		SecurityConfig securityConfig = new SecurityConfig();
		Instant now = Instant.now();
		String token = securityConfig.jwtEncoder(SIGNING_KEY).encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
				JwtClaimsSet.builder().issuer("promptdrive").audience(List.of("promptdrive-api"))
						.subject("1").issuedAt(now).expiresAt(now.plus(1, ChronoUnit.MINUTES))
						.claim("member_id", "1").claim("token_type", "access").build())).getTokenValue();

		Jwt decoded = securityConfig.jwtDecoder(SIGNING_KEY, "promptdrive", "promptdrive-api").decode(token);

		assertThat(decoded.getClaimAsString("member_id")).isEqualTo("1");
	}

	@Test
	void rejectsRefreshTokensAtTheBearerJwtBoundary() {
		SecurityConfig securityConfig = new SecurityConfig();
		Instant now = Instant.now();
		String token = securityConfig.jwtEncoder(SIGNING_KEY).encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
				JwtClaimsSet.builder().issuer("promptdrive").audience(List.of("promptdrive-api"))
						.subject("1").issuedAt(now).expiresAt(now.plus(1, ChronoUnit.MINUTES))
						.claim("member_id", "1").claim("token_type", "refresh").build())).getTokenValue();

		assertThatThrownBy(() -> securityConfig.jwtDecoder(SIGNING_KEY, "promptdrive", "promptdrive-api").decode(token))
				.isInstanceOf(JwtValidationException.class);
	}
}
