package com.chaean.promptdrive.prompt.internal.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.chaean.promptdrive.prompt.internal.application.catalog.CuratedPromptCommandService;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
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
@DisplayName("Prompt ranking MySQL 통합 테스트")
class PromptRankingMySqlIntegrationTest {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CuratedPromptCommandService curatedPromptCommandService;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.update("delete from prompt_like");
		jdbcTemplate.update("delete from prompt");
	}

	@Test
	@DisplayName("좋아요 수, createdAt, id 순으로 정렬하고 0개 좋아요 Prompt도 포함한다")
	void ordersByLikesThenCreatedAtAndId() throws Exception {
		long oldest = createPrompt("oldest");
		long lowerId = createPrompt("tie-lower-id");
		long higherId = createPrompt("tie-higher-id");
		long mostLiked = createPrompt("most-liked");

		LocalDateTime sameCreatedAt = LocalDateTime.of(2024, 1, 1, 0, 0);
		setCreatedAt(oldest, sameCreatedAt.minusDays(1));
		setCreatedAt(lowerId, sameCreatedAt);
		setCreatedAt(higherId, sameCreatedAt);
		setCreatedAt(mostLiked, sameCreatedAt.minusDays(2));
		insertActiveLike(mostLiked, 1L);
		insertActiveLike(mostLiked, 2L);

		mockMvc.perform(get("/api/prompts/rankings"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(mostLiked))
			.andExpect(jsonPath("$.content[0].likeCount").value(2))
			.andExpect(jsonPath("$.content[1].id").value(higherId))
			.andExpect(jsonPath("$.content[2].id").value(lowerId))
			.andExpect(jsonPath("$.content[3].id").value(oldest))
			.andExpect(jsonPath("$.content[3].likeCount").value(0));
	}

	@Test
	@DisplayName("숨김·삭제 Prompt와 soft-deleted like는 ranking에서 제외한다")
	void excludesHiddenDeletedPromptsAndDeletedLikes() throws Exception {
		long active = createPrompt("active");
		long hidden = createPrompt("hidden", PromptVisibility.HIDDEN);
		long deleted = createPrompt("deleted");
		curatedPromptCommandService.deleteCuratedPrompt(deleted);

		insertSoftDeletedLike(active, 7L);
		insertActiveLike(hidden, 8L);
		insertActiveLike(deleted, 9L);

		mockMvc.perform(get("/api/prompts/rankings"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.id == " + active + ")].likeCount").value(0))
			.andExpect(jsonPath("$.content[?(@.id == " + hidden + ")]").doesNotExist())
			.andExpect(jsonPath("$.content[?(@.id == " + deleted + ")]").doesNotExist());
	}

	@Test
	@DisplayName("페이지네이션과 잘못된 bounds를 처리한다")
	void handlesPaginationAndInvalidBounds() throws Exception {
		for (int i = 0; i < 3; i++) {
			createPrompt("page-" + i);
		}

		mockMvc.perform(get("/api/prompts/rankings").param("page", "0").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.hasNext").value(true));

		mockMvc.perform(get("/api/prompts/rankings").param("page", "-1"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/prompts/rankings").param("size", "0"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/prompts/rankings").param("size", "101"))
			.andExpect(status().isBadRequest());
	}

	private long createPrompt(String title) {
		return createPrompt(title, PromptVisibility.PUBLIC);
	}

	private long createPrompt(String title, PromptVisibility visibility) {
		return curatedPromptCommandService.createCuratedPrompt(new CreateCuratedPromptRequest(
			title + "-" + System.nanoTime(), "content", List.of(PromptCategoryType.DEVELOPMENT),
			visibility, null, null)).getId();
	}

	private void setCreatedAt(long promptId, LocalDateTime createdAt) {
		jdbcTemplate.update("update prompt set created_at = ?, updated_at = ? where id = ?",
			createdAt, createdAt, promptId);
	}

	private void insertActiveLike(long promptId, long memberId) {
		jdbcTemplate.update("insert into prompt_like (prompt_id, member_id, created_at, updated_at, deleted_at) "
			+ "values (?, ?, now(), now(), null)", promptId, memberId);
	}

	private void insertSoftDeletedLike(long promptId, long memberId) {
		jdbcTemplate.update("insert into prompt_like (prompt_id, member_id, created_at, updated_at, deleted_at) "
			+ "values (?, ?, now(), now(), now())", promptId, memberId);
	}
}
