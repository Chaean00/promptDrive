package com.chaean.promptdrive.prompt.internal.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.chaean.promptdrive.prompt.internal.application.bookmark.PromptBookmarkCommandService;
import com.chaean.promptdrive.prompt.internal.application.collection.PublicPromptCollectionQueryService;
import com.chaean.promptdrive.prompt.internal.application.collection.PromptCollectionCommandService;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.CreatePromptCollectionRequest;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionItem;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionItemRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

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
	private PromptBookmarkCommandService promptBookmarkCommandService;

	@Autowired
	private PromptCollectionCommandService promptCollectionCommandService;

	@Autowired
	private PublicPromptCollectionQueryService publicPromptCollectionQueryService;

	@Autowired
	private PromptCollectionRepository promptCollectionRepository;

	@Autowired
	private PromptCollectionItemRepository promptCollectionItemRepository;

	@Autowired
	private PromptRepository promptRepository;

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
		return tokenFor(role, "1");
	}

	private String tokenFor(String role, String memberId) {
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
			.andExpect(jsonPath("$.content[0].title").value("integration-title"))
			.andExpect(jsonPath("$.content[0].preview").value("integration-content"));
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
	@DisplayName("공개 Prompt 목록은 공백을 정리한 제한 길이 미리보기를 제공한다")
	void providesNormalizedPromptPreviews() throws Exception {
		String content = "  첫 줄\n\n둘째 줄\t셋째 줄  ";
		curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"preview-title", content, List.of(PromptCategoryType.DEVELOPMENT), PromptVisibility.PUBLIC, null, null));

		mockMvc.perform(get("/api/prompts").param("keyword", "preview-title"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].preview").value("첫 줄 둘째 줄 셋째 줄"));
		curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"preview-limited", "  " + "가".repeat(170) + "\n", List.of(PromptCategoryType.DEVELOPMENT), PromptVisibility.PUBLIC, null, null));
		mockMvc.perform(get("/api/prompts").param("keyword", "preview-limited"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].preview").value("가".repeat(160)));
		String emojiBoundaryContent = "가".repeat(159) + "😀" + "나";
		curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"preview-emoji-boundary", emojiBoundaryContent, List.of(PromptCategoryType.DEVELOPMENT), PromptVisibility.PUBLIC, null, null));
		mockMvc.perform(get("/api/prompts").param("keyword", "preview-emoji-boundary"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].preview").value("가".repeat(159) + "😀"));
	}

	@Test
	@DisplayName("회원은 공개 Prompt를 저장·해제하고 최신 저장 순으로 조회한다")
	void savesAndListsPublicPromptBookmarks() throws Exception {
		long firstPromptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"bookmark-first", "first content", List.of(PromptCategoryType.DEVELOPMENT), PromptVisibility.PUBLIC, null, null)).getId();
		long secondPromptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"bookmark-second", "second content", List.of(PromptCategoryType.TESTING), PromptVisibility.PUBLIC, null, null)).getId();

		mockMvc.perform(post("/api/prompts/{promptId}/bookmarks", firstPromptId).with(csrf()))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/prompts/{promptId}/bookmarks", firstPromptId)
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.promptId").value(firstPromptId))
			.andExpect(jsonPath("$.data.bookmarked").value(true));
		mockMvc.perform(post("/api/prompts/{promptId}/bookmarks", secondPromptId)
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bookmarked").value(true));
		mockMvc.perform(post("/api/prompts/{promptId}/bookmarks", secondPromptId)
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bookmarked").value(true));

		mockMvc.perform(get("/api/my/bookmarked-prompts")
				.header("Authorization", "Bearer " + memberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].id").value(secondPromptId))
			.andExpect(jsonPath("$.content[0].preview").value("second content"));
		mockMvc.perform(post("/api/my/bookmark-statuses")
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"promptIds\":[" + firstPromptId + "," + secondPromptId + "]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsInAnyOrder((int)firstPromptId, (int)secondPromptId)));

		mockMvc.perform(delete("/api/prompts/{promptId}/bookmarks", secondPromptId)
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bookmarked").value(false));
		mockMvc.perform(get("/api/my/bookmarked-prompts")
				.header("Authorization", "Bearer " + memberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(firstPromptId));
		mockMvc.perform(post("/api/prompts/{promptId}/bookmarks", secondPromptId)
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bookmarked").value(true));
		mockMvc.perform(get("/api/my/bookmarked-prompts")
				.header("Authorization", "Bearer " + memberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(secondPromptId));

		curatedPromptCommandService.changeCuratedPromptVisibility(firstPromptId, PromptVisibility.HIDDEN);
		mockMvc.perform(post("/api/my/bookmark-statuses")
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"promptIds\":[" + firstPromptId + "," + secondPromptId + "]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0]").value(secondPromptId));
		mockMvc.perform(get("/api/my/bookmarked-prompts")
				.header("Authorization", "Bearer " + memberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(secondPromptId));
		mockMvc.perform(post("/api/prompts/{promptId}/bookmarks", firstPromptId)
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/prompts/{promptId}/bookmarks", firstPromptId)
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bookmarked").value(false));
		curatedPromptCommandService.changeCuratedPromptVisibility(firstPromptId, PromptVisibility.PUBLIC);
		mockMvc.perform(get("/api/my/bookmarked-prompts")
				.header("Authorization", "Bearer " + memberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(secondPromptId));
		mockMvc.perform(post("/api/my/bookmark-statuses")
				.header("Authorization", "Bearer " + memberToken)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"promptIds\":[" + firstPromptId + "," + secondPromptId + "]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0]").value(secondPromptId));
	}

	@Test
	@DisplayName("회원은 저장한 Prompt를 다음 페이지까지 조회한다")
	void pagesThroughMoreThanTwentyBookmarks() throws Exception {
		String isolatedMemberToken = tokenFor("MEMBER", "2");
		List<Long> promptIds = java.util.stream.IntStream.range(0, 21)
			.mapToObj(index -> curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
				"bookmark-page-" + UUID.randomUUID(), "content-" + index, List.of(PromptCategoryType.DEVELOPMENT), PromptVisibility.PUBLIC, null, null)).getId())
			.toList();
		for (Long promptId : promptIds) {
			mockMvc.perform(post("/api/prompts/{promptId}/bookmarks", promptId)
					.header("Authorization", "Bearer " + isolatedMemberToken)
					.with(csrf()))
				.andExpect(status().isOk());
		}

		mockMvc.perform(get("/api/my/bookmarked-prompts").param("page", "0").param("size", "20")
				.header("Authorization", "Bearer " + isolatedMemberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(20))
			.andExpect(jsonPath("$.hasNext").value(true));
		mockMvc.perform(get("/api/my/bookmarked-prompts").param("page", "1").param("size", "20")
				.header("Authorization", "Bearer " + isolatedMemberToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	@DisplayName("동시에 같은 Prompt를 저장해도 활성 북마크는 하나만 남는다")
	void keepsOneActiveBookmarkDuringConcurrentSaves() throws Exception {
		long promptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"bookmark-concurrent", "content", List.of(PromptCategoryType.DEVELOPMENT), PromptVisibility.PUBLIC, null, null)).getId();
		ExecutorService executor = Executors.newFixedThreadPool(8);
		CountDownLatch ready = new CountDownLatch(8);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Object>> saves = java.util.stream.IntStream.range(0, 8)
				.mapToObj(index -> executor.submit(() -> {
					ready.countDown();
					start.await();
					promptBookmarkCommandService.registerPromptBookmark(3L, promptId);
					return null;
				}))
				.toList();
			assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
			start.countDown();
			for (Future<?> save : saves) {
				save.get();
			}
			Integer activeBookmarks = jdbcTemplate.queryForObject(
				"select count(*) from prompt_bookmark where prompt_id = ? and member_id = ? and deleted_at is null", Integer.class, promptId, 3L);
			assertThat(activeBookmarks).isEqualTo(1);
		} finally {
			executor.shutdownNow();
		}
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
		mockMvc.perform(get("/api/prompts")
				.param("keyword", "query-count-title-")
				.param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.last").value(false));

		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().clear();
		publicPromptQueryService.getPublicPromptPage(null, null, 0, 100);

		org.assertj.core.api.Assertions.assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("공개 Collection은 공개·미삭제 Prompt만 외부에 노출한다")
	void exposesOnlyPublicPromptsFromCollection() throws Exception {
		long publicPromptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"collection-public", "content", List.of(PromptCategoryType.DEVELOPMENT), PromptVisibility.PUBLIC, null, null)).getId();
		promptCollectionCommandService.create(new CreatePromptCollectionRequest(
			"collection-mvp", "MVP collection", "Public acquisition", List.of(publicPromptId)));

		mockMvc.perform(get("/api/prompt-collections/collection-mvp"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.prompts.length()").value(1))
			.andExpect(jsonPath("$.data.prompts[0].id").value(publicPromptId));

		curatedPromptCommandService.changeCuratedPromptVisibility(publicPromptId, PromptVisibility.HIDDEN);
		assertThat(publicPromptCollectionQueryService.getBySlug("collection-mvp").getPrompts()).isEmpty();
	}

	@Test
	@DisplayName("공개 Collection 목록은 제목·설명과 공개 Prompt 수를 제공한다")
	void listsCollectionsWithVisiblePromptCounts() throws Exception {
		long visiblePromptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"collection-list-visible", "content", List.of(PromptCategoryType.DEVELOPMENT),
			PromptVisibility.PUBLIC, null, null)).getId();
		long hiddenPromptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"collection-list-hidden", "content", List.of(PromptCategoryType.TESTING),
			PromptVisibility.PUBLIC, null, null)).getId();
		promptCollectionCommandService.create(new CreatePromptCollectionRequest(
			"collection-visible", "Visible collection", "Visible description", List.of(visiblePromptId)));
		promptCollectionCommandService.create(new CreatePromptCollectionRequest(
			"collection-hidden", "Hidden collection", "Hidden description", List.of(hiddenPromptId)));
		curatedPromptCommandService.changeCuratedPromptVisibility(hiddenPromptId, PromptVisibility.HIDDEN);

		mockMvc.perform(get("/api/prompt-collections"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[?(@.slug == 'collection-visible')].title")
				.value(org.hamcrest.Matchers.contains("Visible collection")))
			.andExpect(jsonPath("$.data[?(@.slug == 'collection-visible')].description")
				.value(org.hamcrest.Matchers.contains("Visible description")))
			.andExpect(jsonPath("$.data[?(@.slug == 'collection-visible')].promptCount")
				.value(org.hamcrest.Matchers.contains(1)))
			.andExpect(jsonPath("$.data[?(@.slug == 'collection-hidden')].promptCount")
				.value(org.hamcrest.Matchers.contains(0)));
	}

	@Test
	@DisplayName("삭제된 Collection slug는 재사용하고 활성 중복 slug는 거부한다")
	void enforcesCollectionSlugUniquenessAcrossSoftDelete() {
		String slug = "collection-" + UUID.randomUUID();
		long promptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"collection-unique-prompt", "content", List.of(PromptCategoryType.DEVELOPMENT),
			PromptVisibility.PUBLIC, null, null)).getId();
		var request = new CreatePromptCollectionRequest(slug, "Collection", "Description", List.of(promptId));
		long collectionId = promptCollectionCommandService.create(request).getId();

		assertThatThrownBy(() -> promptCollectionCommandService.create(request))
			.isInstanceOf(DataIntegrityViolationException.class);

		promptCollectionCommandService.delete(collectionId);
		assertThat(promptCollectionCommandService.create(request).getSlug()).isEqualTo(slug);
	}

	@Test
	@DisplayName("활성 Collection item 중복은 거부하고 삭제된 item은 재등록한다")
	void enforcesCollectionItemUniquenessAcrossSoftDelete() {
		long promptId = curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"collection-item-unique-prompt", "content", List.of(PromptCategoryType.DEVELOPMENT),
			PromptVisibility.PUBLIC, null, null)).getId();
		var collection = promptCollectionCommandService.create(new CreatePromptCollectionRequest(
			"collection-item-" + UUID.randomUUID(), "Collection", "Description", List.of(promptId)));
		var collectionEntity = promptCollectionRepository.findById(collection.getId()).orElseThrow();
		var prompt = promptRepository.findById(promptId).orElseThrow();

		assertThatThrownBy(() -> promptCollectionItemRepository.saveAndFlush(
			PromptCollectionItem.create(collectionEntity, prompt, 1)))
			.isInstanceOf(DataIntegrityViolationException.class);

		var existingItem = promptCollectionItemRepository.findAllByCollectionId(collection.getId()).getFirst();
		existingItem.softDelete();
		promptCollectionItemRepository.saveAndFlush(existingItem);
		assertThat(promptCollectionItemRepository.saveAndFlush(
			PromptCollectionItem.create(collectionEntity, prompt, 1)).getPrompt().getId()).isEqualTo(promptId);
	}
}
