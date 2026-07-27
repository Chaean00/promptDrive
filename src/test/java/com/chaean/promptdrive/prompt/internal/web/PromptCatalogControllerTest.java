package com.chaean.promptdrive.prompt.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.chaean.promptdrive.prompt.internal.application.catalog.BrowsePromptService;
import com.chaean.promptdrive.prompt.internal.application.catalog.GetPublicPromptService;
import com.chaean.promptdrive.prompt.internal.application.catalog.MaintainCuratedPromptService;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.CuratedPromptResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.persistence.Prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chaean.promptdrive.prompt.internal.web.catalog.AdminPromptController;
import com.chaean.promptdrive.prompt.internal.web.catalog.PublicPromptController;
import com.chaean.promptdrive.prompt.internal.web.category.CategoryController;

@ExtendWith(MockitoExtension.class)
class PromptCatalogControllerTest {

	@Mock
	private BrowsePromptService browsePromptService;

	@Mock
	private GetPublicPromptService getPublicPromptService;

	@Mock
	private MaintainCuratedPromptService maintainCuratedPromptService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(
			new PublicPromptController(browsePromptService, getPublicPromptService),
			new CategoryController(),
			new AdminPromptController(maintainCuratedPromptService)
		).build();
	}

	@Test
	void returnsFixedCategoryDisplayMetadata() throws Exception {
		mockMvc.perform(get("/api/prompt-categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].code").value("DEVELOPMENT"))
			.andExpect(jsonPath("$.data[0].englishName").value("Development"))
			.andExpect(jsonPath("$.data[0].koreanName").value("개발"));
	}

	@Test
	void exposesPublicBrowseAndDetailContracts() throws Exception {
		when(browsePromptService.browse(null, null, 0, 20))
			.thenReturn(com.chaean.promptdrive.common.web.response.SliceResponse.from(new SliceImpl<>(List.of())));
		Prompt prompt = Prompt.createCurated("title", "content", PromptVisibility.PUBLIC, null, null);
		when(getPublicPromptService.get(1L)).thenReturn(PromptDetailResponse.from(prompt, List.of()));

		mockMvc.perform(get("/api/prompts").param("page", "0").param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.page").value(0));
		mockMvc.perform(get("/api/prompts/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("title"))
			.andExpect(jsonPath("$.data.provenance.code").value("CURATED"));
	}

	@Test
	void createsAndDeletesCuratedPromptThroughAdminContract() throws Exception {
		Prompt prompt = Prompt.createCurated("title", "content", PromptVisibility.PUBLIC, null, null);
		CuratedPromptResponse response = CuratedPromptResponse.from(prompt, List.of());
		when(maintainCuratedPromptService.create(any(CreateCuratedPromptRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/admin/prompts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"title","content":"content","categories":["DEVELOPMENT"],"visibility":"PUBLIC"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.visibility.code").value("PUBLIC"));

		mockMvc.perform(delete("/api/admin/prompts/1"))
			.andExpect(status().isNoContent());
		verify(maintainCuratedPromptService).delete(1L);
	}
}
