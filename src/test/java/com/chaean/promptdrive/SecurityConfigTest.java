package com.chaean.promptdrive;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import com.chaean.promptdrive.common.config.SecurityConfig;

class SecurityConfigTest {

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
}
