package com.chaean.promptdrive.prompt.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

import com.chaean.promptdrive.prompt.internal.application.catalog.CuratedPromptCommandService;
import com.chaean.promptdrive.prompt.internal.application.like.PromptLikeCommandService;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
@DisplayName("Prompt 좋아요 MySQL 통합 테스트")
class PromptLikeMySqlIntegrationTest {

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
	private CuratedPromptCommandService curatedPromptCommandService;

	@Autowired
	private PromptLikeCommandService promptLikeCommandService;

	private String memberToken;

	@BeforeEach
	void setUpToken() {
		memberToken = tokenFor("7");
	}

	@Test
	@DisplayName("인증, 공개 여부, 중복 요청, soft delete와 재좋아요를 검증한다")
	void managesPublicPromptLikeLifecycle() throws Exception {
		long promptId = createPrompt(PromptVisibility.PUBLIC);

		mockMvc.perform(post("/api/prompts/{promptId}/likes", promptId).with(csrf()))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/prompts/{promptId}/likes", promptId).with(csrf()))
			.andExpect(status().isUnauthorized());

		registerPromptLike(promptId);
		registerPromptLike(promptId);
		assertLikeCounts(promptId, 1, 1);

		Long ownerLikeId = activeLikeId(promptId, 7L);
		removePromptLike(promptId);
		removePromptLike(promptId);
		assertLikeCounts(promptId, 0, 1);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_like where prompt_id = ? and member_id = ? and deleted_at is not null",
			Integer.class, promptId, 7L)).isEqualTo(1);

		registerPromptLike(promptId);
		assertLikeCounts(promptId, 1, 1);
		assertThat(activeLikeId(promptId, 7L)).isEqualTo(ownerLikeId);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_like where prompt_id = ? and member_id = ?", Integer.class, promptId, 7L))
			.isEqualTo(1);
	}

	@Test
	@DisplayName("비공개 및 삭제된 Prompt는 좋아요를 거부한다")
	void rejectsHiddenAndDeletedPrompts() throws Exception {
		long hiddenPromptId = createPrompt(PromptVisibility.HIDDEN);
		long deletedPromptId = createPrompt(PromptVisibility.PUBLIC);
		curatedPromptCommandService.deleteCuratedPrompt(deletedPromptId);

		for (long promptId : List.of(hiddenPromptId, deletedPromptId)) {
			mockMvc.perform(post("/api/prompts/{promptId}/likes", promptId)
				.header("Authorization", "Bearer " + memberToken).with(csrf()))
				.andExpect(status().isNotFound());
			mockMvc.perform(delete("/api/prompts/{promptId}/likes", promptId)
				.header("Authorization", "Bearer " + memberToken).with(csrf()))
				.andExpect(status().isNotFound());
		}
	}

	@Test
	@DisplayName("동시 최초 좋아요는 하나의 활성 행만 만든다")
	void createsOneLikeForConcurrentFirstLikes() throws Exception {
		long promptId = createPrompt(PromptVisibility.PUBLIC);
		CyclicBarrier barrier = new CyclicBarrier(2);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			var first = executor.submit(() -> {
				barrier.await();
				return promptLikeCommandService.registerPromptLike(7L, promptId);
			});
			var second = executor.submit(() -> {
				barrier.await();
				return promptLikeCommandService.registerPromptLike(7L, promptId);
			});

			assertThat(first.get().isLiked()).isTrue();
			assertThat(second.get().isLiked()).isTrue();
			assertLikeCounts(promptId, 1, 1);
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	@DisplayName("활성 및 삭제된 이력이 섞여도 기존 활성 좋아요를 유지한다")
	void keepsExistingActiveLikeWhenDeletedHistoryExists() throws Exception {
		long promptId = createPrompt(PromptVisibility.PUBLIC);
		promptLikeCommandService.registerPromptLike(7L, promptId);
		Long activeLikeId = activeLikeId(promptId, 7L);
		jdbcTemplate.update(
			"insert into prompt_like (prompt_id, member_id, deleted_at) values (?, ?, current_timestamp(6))",
			promptId, 7L);

		registerPromptLike(promptId);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_like where prompt_id = ? and member_id = ? and deleted_at is null",
			Integer.class, promptId, 7L)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_like where prompt_id = ? and member_id = ?",
			Integer.class, promptId, 7L)).isEqualTo(2);
		assertThat(activeLikeId(promptId, 7L)).isEqualTo(activeLikeId);
	}

	private void registerPromptLike(long promptId) throws Exception {
		mockMvc.perform(post("/api/prompts/{promptId}/likes", promptId)
			.header("Authorization", "Bearer " + memberToken).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.promptId").value(promptId))
			.andExpect(jsonPath("$.data.liked").value(true));
	}

	private void removePromptLike(long promptId) throws Exception {
		mockMvc.perform(delete("/api/prompts/{promptId}/likes", promptId)
			.header("Authorization", "Bearer " + memberToken).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.liked").value(false));
	}

	private long createPrompt(PromptVisibility visibility) {
		return curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			"like-test-" + visibility + "-" + System.nanoTime(), "content", List.of(), visibility, null, null)).getId();
	}

	private void assertLikeCounts(long promptId, int activeCount, int totalCount) {
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_like where prompt_id = ? and deleted_at is null", Integer.class, promptId))
			.isEqualTo(activeCount);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from prompt_like where prompt_id = ?", Integer.class, promptId))
			.isEqualTo(totalCount);
	}

	private Long activeLikeId(long promptId, long memberId) {
		return jdbcTemplate.queryForObject(
			"select id from prompt_like where prompt_id = ? and member_id = ? and deleted_at is null",
			Long.class, promptId, memberId);
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
