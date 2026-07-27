package com.chaean.promptdrive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaean.promptdrive.common.config.SecurityConfig;
import com.chaean.promptdrive.common.config.JwtProperties;
import com.chaean.promptdrive.common.config.JwtSigningKeyFactory;

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
		String token = securityConfig.jwtEncoder(new JwtSigningKeyFactory(), jwtProperties()).encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
				JwtClaimsSet.builder().issuer("promptdrive").audience(List.of("promptdrive-api"))
						.subject("1").issuedAt(now).expiresAt(now.plus(1, ChronoUnit.MINUTES))
						.claim("member_id", "1").claim("token_type", "access").build())).getTokenValue();

		Jwt decoded = securityConfig.jwtDecoder(new JwtSigningKeyFactory(), jwtProperties()).decode(token);

		assertThat(decoded.getClaimAsString("member_id")).isEqualTo("1");
	}

	@Test
	void rejectsRefreshTokensAtTheBearerJwtBoundary() {
		SecurityConfig securityConfig = new SecurityConfig();
		Instant now = Instant.now();
		String token = securityConfig.jwtEncoder(new JwtSigningKeyFactory(), jwtProperties()).encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
				JwtClaimsSet.builder().issuer("promptdrive").audience(List.of("promptdrive-api"))
						.subject("1").issuedAt(now).expiresAt(now.plus(1, ChronoUnit.MINUTES))
						.claim("member_id", "1").claim("token_type", "refresh").build())).getTokenValue();

		assertThatThrownBy(() -> securityConfig.jwtDecoder(new JwtSigningKeyFactory(), jwtProperties()).decode(token))
				.isInstanceOf(JwtValidationException.class);
	}

	private JwtProperties jwtProperties() {
		JwtProperties properties = new JwtProperties();
		properties.setSigningKey(SIGNING_KEY);
		properties.setIssuer("promptdrive");
		properties.setAudience("promptdrive-api");
		return properties;
	}

	@Nested
	@WebMvcTest(controllers = SecurityTestController.class, properties = {
			"security.jwt.signing-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
			"security.jwt.issuer=promptdrive",
			"security.jwt.audience=promptdrive-api"
	})
	@Import({SecurityConfig.class, JwtSigningKeyFactory.class, SecurityTestController.class})
	class SecurityFilterChainTests {

		@Autowired
		private org.springframework.test.web.servlet.MockMvc mockMvc;

		@Test
		void permitsOnlyPublicGetEndpointsForAnonymousUsers() throws Exception {
			mockMvc.perform(get("/api/prompts"))
					.andExpect(status().isOk());
			mockMvc.perform(get("/api/prompts/1"))
					.andExpect(status().isOk());
			mockMvc.perform(get("/api/prompt-categories"))
					.andExpect(status().isOk());

			mockMvc.perform(get("/api/prompt-categories/1"))
					.andExpect(status().isUnauthorized());
			mockMvc.perform(post("/api/prompts"))
					.andExpect(status().isForbidden());
			mockMvc.perform(get("/api/private"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void permitsAdminRoleForExactAndNestedAdminPromptUrls() throws Exception {
			mockMvc.perform(get("/api/admin/prompts").with(user("admin").roles("ADMIN")))
					.andExpect(status().isOk());
			mockMvc.perform(get("/api/admin/prompts/1").with(user("admin").roles("ADMIN")))
					.andExpect(status().isOk());

			mockMvc.perform(get("/api/admin/prompts").with(user("member").roles("MEMBER")))
					.andExpect(status().isForbidden());

			mockMvc.perform(post("/api/admin/prompts").with(csrf()))
				.andExpect(status().isUnauthorized());
			mockMvc.perform(post("/api/admin/prompts").with(user("member").roles("MEMBER")).with(csrf()))
				.andExpect(status().isForbidden());
			mockMvc.perform(post("/api/admin/prompts").with(user("admin").roles("ADMIN")))
				.andExpect(status().isForbidden());
			mockMvc.perform(post("/api/admin/prompts")
					.with(user("admin").roles("ADMIN"))
					.with(csrf().useInvalidToken()))
				.andExpect(status().isForbidden());
			mockMvc.perform(post("/api/admin/prompts").with(user("admin").roles("ADMIN")).with(csrf()))
				.andExpect(status().isOk());
		}
	}

	@RestController
	static class SecurityTestController {

		@GetMapping({
				"/api/prompts", "/api/prompts/{id}", "/api/prompt-categories", "/api/prompt-categories/{id}",
				"/api/admin/prompts", "/api/admin/prompts/{id}", "/api/private"
		})
		String get() {
			return "ok";
		}

		@PostMapping("/api/prompts")
		String createPrompt() {
			return "ok";
		}

		@PostMapping("/api/admin/prompts")
		String createAdminPrompt() {
			return "ok";
		}
	}
}
