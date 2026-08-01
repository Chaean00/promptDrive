package com.chaean.promptdrive.prompt.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
	"spring.flyway.enabled=true",
	"spring.jpa.hibernate.ddl-auto=validate",
	"security.jwt.signing-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
	"security.jwt.issuer=promptdrive",
	"security.jwt.audience=promptdrive-api"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("커뮤니티 Prompt MySQL 통합 테스트")
class CommunityPromptMySqlIntegrationTest {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	private String ownerToken;
	private String otherMemberToken;

	@BeforeEach
	void setUpTokens() {
		ownerToken = tokenFor("1");
		otherMemberToken = tokenFor("2");
	}

	@Test
	@DisplayName("소유자 lifecycle과 타인 접근 차단 및 소프트 삭제를 검증한다")
	void verifiesOwnerLifecycleAndIdorProtection() throws Exception {
		mockMvc.perform(post("/api/my/prompts").with(csrf()))
			.andExpect(status().isUnauthorized());

		MvcResult created = mockMvc.perform(post("/api/my/prompts")
				.header("Authorization", "Bearer " + ownerToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"community-title","content":"community-content","categories":["DEVELOPMENT","DATABASE"]}
					"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.provenance.code").value("COMMUNITY"))
				.andReturn();
		long promptId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

		assertThat(jdbcTemplate.queryForObject("select owner_member_id from prompt where id = ?", Long.class, promptId))
			.isEqualTo(1L);
		assertThat(jdbcTemplate.queryForObject("select provenance from prompt where id = ?", String.class, promptId))
			.isEqualTo("COMMUNITY");
		assertThat(jdbcTemplate.queryForObject("select visibility from prompt where id = ?", String.class, promptId))
			.isEqualTo("PUBLIC");
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_category where prompt_id = ? and deleted_at is null", Integer.class, promptId))
			.isEqualTo(2);

		mockMvc.perform(get("/api/my/prompts").header("Authorization", "Bearer " + ownerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(promptId));
		mockMvc.perform(get("/api/my/prompts/{id}", promptId).header("Authorization", "Bearer " + ownerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(promptId));

		mockMvc.perform(get("/api/my/prompts/{id}", promptId).header("Authorization", "Bearer " + otherMemberToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(put("/api/my/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + otherMemberToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"other\",\"content\":\"other\",\"categories\":[]}"))
			.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/my/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + otherMemberToken).with(csrf()))
			.andExpect(status().isNotFound());
		assertThat(jdbcTemplate.queryForObject("select title from prompt where id = ?", String.class, promptId))
			.isEqualTo("community-title");
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_category where prompt_id = ? and deleted_at is null", Integer.class, promptId))
			.isEqualTo(2);

		mockMvc.perform(put("/api/my/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + ownerToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"community-updated","content":"community-updated-content","categories":["DATABASE","TESTING"]}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("community-updated"));
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_category where prompt_id = ? and category = 'DEVELOPMENT' and deleted_at is not null",
			Integer.class, promptId)).isEqualTo(1);

		mockMvc.perform(delete("/api/my/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + ownerToken).with(csrf()))
			.andExpect(status().isNoContent());
		assertThat(jdbcTemplate.queryForObject("select count(*) from prompt where id = ? and deleted_at is not null", Integer.class,
			promptId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_category where prompt_id = ? and deleted_at is not null", Integer.class, promptId))
			.isEqualTo(3);
		mockMvc.perform(get("/api/prompts/{id}", promptId))
			.andExpect(status().isNotFound());
	}

	private String tokenFor(String memberId) {
		Instant now = Instant.now();
		return jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
			JwtClaimsSet.builder()
				.issuer("promptdrive")
				.audience(List.of("promptdrive-api"))
				.subject(memberId)
				.issuedAt(now)
				.expiresAt(now.plus(5, ChronoUnit.MINUTES))
				.claim("member_id", memberId)
				.claim("roles", List.of("MEMBER"))
				.claim("token_type", "access")
				.build())).getTokenValue();
	}
}
