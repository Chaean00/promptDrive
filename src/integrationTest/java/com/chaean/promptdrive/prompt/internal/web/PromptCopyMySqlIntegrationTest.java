package com.chaean.promptdrive.prompt.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import com.chaean.promptdrive.prompt.internal.application.catalog.CuratedPromptCommandService;
import com.chaean.promptdrive.prompt.internal.application.copy.PromptCopyCommandService;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;

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
@DisplayName("Prompt copy count MySQL 통합 테스트")
class PromptCopyMySqlIntegrationTest {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CuratedPromptCommandService curatedPromptCommandService;

	@Autowired
	private PromptCopyCommandService promptCopyCommandService;

	@Test
	@DisplayName("새로 생성한 공개 Prompt 상세 응답의 copyCount는 0이다")
	void exposesZeroCopyCountForNewPublicPrompt() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/admin/prompts")
				.header("Authorization", "Bearer " + tokenForAdmin())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"copy-count-title\",\"content\":\"copy-count-content\",\"categories\":[\"DEVELOPMENT\"],\"visibility\":\"PUBLIC\"}"))
			.andExpect(status().isCreated())
			.andReturn();

		long promptId = objectMapper.readTree(created.getResponse().getContentAsString())
			.get("data").get("id").asLong();

		mockMvc.perform(get("/api/prompts/{id}", promptId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.copyCount").value(0));
	}

	@Test
	@DisplayName("큐레이션 Prompt 상세 응답은 저장된 copyCount를 유지한다")
	void preservesNonZeroCopyCountForCuratedPrompt() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/admin/prompts")
				.header("Authorization", "Bearer " + tokenForAdmin())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"curated-copy-count-title\",\"content\":\"curated-copy-count-content\",\"categories\":[\"DEVELOPMENT\"],\"visibility\":\"PUBLIC\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		long promptId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();
		jdbcTemplate.update("update prompt set copy_count = 7 where id = ?", promptId);

		mockMvc.perform(get("/api/admin/prompts/{id}", promptId)
				.header("Authorization", "Bearer " + tokenForAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.copyCount").value(7));
	}

	@Test
	@DisplayName("로그인하지 않은 사용자는 CSRF 토큰으로 공개 Prompt를 복사할 수 있다")
	void copiesPublicPromptAnonymously() throws Exception {
		long promptId = createPrompt(PromptVisibility.PUBLIC);

		mockMvc.perform(post("/api/prompts/{id}/copies", promptId).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.promptId").value(promptId))
			.andExpect(jsonPath("$.data.copyCount").value(1));
	}

	@Test
	@DisplayName("비공개 및 삭제된 Prompt 복사는 거부한다")
	void rejectsHiddenAndDeletedPrompts() throws Exception {
		long hiddenPromptId = createPrompt(PromptVisibility.HIDDEN);
		long deletedPromptId = createPrompt(PromptVisibility.PUBLIC);
		curatedPromptCommandService.deleteCuratedPrompt(deletedPromptId);

		for (long promptId : List.of(hiddenPromptId, deletedPromptId)) {
			mockMvc.perform(post("/api/prompts/{id}/copies", promptId).with(csrf()))
				.andExpect(status().isNotFound());
		}
	}

	@Test
	@DisplayName("동시 복사 요청의 최종 copyCount는 요청 수와 같다")
	void incrementsCopyCountAtomicallyUnderConcurrency() throws Exception {
		long promptId = createPrompt(PromptVisibility.PUBLIC);
		int calls = 8;
		CyclicBarrier barrier = new CyclicBarrier(calls);
		ExecutorService executor = Executors.newFixedThreadPool(calls);

		try {
			var futures = java.util.stream.IntStream.range(0, calls)
				.mapToObj(index -> executor.submit(() -> {
					barrier.await();
					return promptCopyCommandService.registerPromptCopy(promptId);
				}))
				.toList();
			for (var future : futures) {
				future.get();
			}
			assertThat(jdbcTemplate.queryForObject("select copy_count from prompt where id = ?", Long.class, promptId))
				.isEqualTo((long) calls);
		}
		finally {
			executor.shutdownNow();
		}
	}

	private long createPrompt(PromptVisibility visibility) {
		return curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"copy-test-" + visibility + "-" + System.nanoTime(), "content", List.of(), visibility, null, null)).getId();
	}

	private String tokenForAdmin() {
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
				.claim("roles", List.of("ADMIN"))
				.claim("token_type", "access")
				.build())).getTokenValue();
	}
}
