package com.chaean.promptdrive.prompt.internal.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import org.hibernate.SessionFactory;

import com.chaean.promptdrive.prompt.internal.application.catalog.PublicPromptQueryService;
import com.chaean.promptdrive.prompt.internal.application.catalog.CuratedPromptCommandService;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = {
	"spring.flyway.enabled=true",
	"spring.jpa.hibernate.ddl-auto=validate",
	"spring.jpa.properties.hibernate.generate_statistics=true",
	"seo.site-url=https://promptdrive.co.kr",
	"security.jwt.signing-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
	"security.jwt.issuer=promptdrive",
	"security.jwt.audience=promptdrive-api"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Prompt catalog MySQL 통합 테스트")
class PromptCatalogMySqlIntegrationTest {

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

	@Autowired
	private PublicPromptQueryService publicPromptQueryService;

	@Autowired
	private CuratedPromptCommandService curatedPromptCommandService;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@PersistenceContext
	private EntityManager entityManager;

	private String adminToken;
	private String memberToken;

	@BeforeEach
	void setUpToken() {
		adminToken = tokenFor("ADMIN");
		memberToken = tokenFor("MEMBER");
	}

	private String tokenFor(String role) {
		Instant now = Instant.now();
		return jwtEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
			JwtClaimsSet.builder()
				.issuer("promptdrive")
				.audience(List.of("promptdrive-api"))
				.subject("1")
				.issuedAt(now)
				.expiresAt(now.plus(5, ChronoUnit.MINUTES))
				.claim("member_id", "1")
				.claim("roles", List.of(role))
				.claim("token_type", "access")
			.build())).getTokenValue();
	}

	@Test
	@DisplayName("Flyway schema에서 고정 category 목록을 제공한다")
	void startsWithFlywayValidatedMySqlSchemaAndServesFixedCategories() throws Exception {
		mockMvc.perform(get("/api/prompt-categories"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("큐레이션 Prompt의 생성·수정·공개·숨김·삭제 lifecycle을 처리한다")
	void completesCuratedAdminVisibilityAndSoftDeleteLifecycle() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/admin/prompts")
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"integration-title","content":"integration-content","categories":["DEVELOPMENT"],"visibility":"PUBLIC"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.visibility.code").value("PUBLIC"))
			.andReturn();

		long promptId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

		mockMvc.perform(get("/api/prompts").param("category", "DEVELOPMENT"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].title").value("integration-title"));
		mockMvc.perform(get("/api/prompts").param("keyword", "integration-content"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].title").value("integration-title"));
		mockMvc.perform(get("/api/prompts")
				.param("provenance", "CURATED")
				.param("category", "DEVELOPMENT"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/prompts").param("provenance", "CURATED"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/prompts").param("category", "UNKNOWN"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/prompts").param("page", "-1"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/prompts").param("size", "0"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/prompts").param("size", "101"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/prompts/999999"))
			.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/admin/prompts").with(csrf()))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/admin/prompts")
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"integration-title-updated","content":"integration-content-updated","categories":["DEVELOPMENT","TESTING"],"sourceName":null,"sourceUrl":null}
					"""))
			.andExpect(status().isOk());
		mockMvc.perform(put("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"integration-title-updated","content":"integration-content-updated","categories":["TESTING"],"sourceName":null,"sourceUrl":null}
					"""))
			.andExpect(status().isOk());
		Integer deletedDuringReplacement = jdbcTemplate.queryForObject(
			"select count(*) from prompt_category where prompt_id = ? and category = 'DEVELOPMENT' and deleted_at is not null",
			Integer.class, promptId);
		org.assertj.core.api.Assertions.assertThat(deletedDuringReplacement).isEqualTo(1);

		mockMvc.perform(patch("/api/admin/prompts/{id}/visibility", promptId)
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"visibility\":\"HIDDEN\"}"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/prompts/{id}", promptId))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.visibility.code").value("HIDDEN"));
		mockMvc.perform(get("/api/admin/prompts/{id}", promptId))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + memberToken))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/prompts").param("visibility", "HIDDEN")
				.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].visibility.code").value("HIDDEN"));

		mockMvc.perform(patch("/api/admin/prompts/{id}/visibility", promptId)
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"visibility\":\"PUBLIC\"}"))
			.andExpect(status().isOk());

		mockMvc.perform(delete("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf()))
			.andExpect(status().isNoContent());

		Integer deletedCategories = jdbcTemplate.queryForObject(
			"select count(*) from prompt_category where prompt_id = ? and deleted_at is not null", Integer.class, promptId);
		org.assertj.core.api.Assertions.assertThat(deletedCategories).isEqualTo(2);
		mockMvc.perform(get("/api/prompts/{id}", promptId))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("공개 Prompt 응답에는 내부 출처 정보를 노출하지 않는다")
	void doesNotExposeSourceMetadataFromPublicPromptResponse() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/admin/prompts")
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"source-hidden-title","content":"source-hidden-content","categories":["DEVELOPMENT"],"visibility":"PUBLIC","sourceName":"internal-source","sourceUrl":"https://example.com/internal-source"}
					"""))
			.andExpect(status().isCreated())
			.andReturn();

		long promptId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

		mockMvc.perform(get("/api/prompts/{id}", promptId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.sourceName").doesNotExist())
			.andExpect(jsonPath("$.data.sourceUrl").doesNotExist());

		mockMvc.perform(get("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + adminToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.sourceName").value("internal-source"))
			.andExpect(jsonPath("$.data.sourceUrl").value("https://example.com/internal-source"));
	}

	@Test
	@DisplayName("공개 Prompt와 카테고리의 정규 URL을 sitemap과 robots에 제공한다")
	void servesPublicSitemapAndRobots() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/admin/prompts")
				.header("Authorization", "Bearer " + adminToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"seo-sitemap-title","content":"seo-sitemap-content","categories":["DEVELOPMENT"],"visibility":"PUBLIC"}
					"""))
			.andExpect(status().isCreated())
			.andReturn();

		long promptId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

		mockMvc.perform(get("/sitemap.xml"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(
				"https://promptdrive.co.kr/prompts/" + promptId)))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(
				"https://promptdrive.co.kr/categories/DEVELOPMENT")));

		mockMvc.perform(get("/robots.txt"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(
				"Sitemap: https://promptdrive.co.kr/sitemap.xml")));
	}

	@Test
	@Transactional
	@DisplayName("Prompt 목록 조회에서 Prompt와 category를 배치 조회한다")
	void loadsBrowsePageWithOnePromptQueryAndOneBatchCategoryQuery() throws Exception {
		for (int index = 0; index < 3; index++) {
			curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
				"query-count-title-" + index, "query-count-content",
				List.of(PromptCategoryType.DEVELOPMENT, PromptCategoryType.TESTING), PromptVisibility.PUBLIC, null, null));
		}
		entityManager.flush();
		entityManager.clear();
		mockMvc.perform(get("/api/prompts").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.last").value(false));

		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().clear();
		publicPromptQueryService.getPublicPromptPage(null, null, 0, 20);

		org.assertj.core.api.Assertions.assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isEqualTo(2);
	}
}
