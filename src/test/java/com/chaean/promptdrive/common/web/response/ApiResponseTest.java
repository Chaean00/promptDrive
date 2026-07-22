package com.chaean.promptdrive.common.web.response;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("공통 성공 응답")
class ApiResponseTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController()).build();
	}

	@Test
	@DisplayName("성공 응답은 data만 최상위 속성으로 노출한다")
	void exposesOnlyDataAtTheTopLevel() throws Exception {
		mockMvc.perform(get("/test/success"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", aMapWithSize(1)))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.status").doesNotExist())
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
	}

	@Test
	@DisplayName("Page를 안정적인 페이징 응답 계약으로 변환한다")
	void convertsPageToStableResponseContract() throws Exception {
		mockMvc.perform(get("/test/page"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data", aMapWithSize(7)))
			.andExpect(jsonPath("$.data.content[0]").value("alpha"))
			.andExpect(jsonPath("$.data.content[1]").value("beta"))
			.andExpect(jsonPath("$.data.page").value(2))
			.andExpect(jsonPath("$.data.size").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(7))
			.andExpect(jsonPath("$.data.totalPages").value(4))
			.andExpect(jsonPath("$.data.first").value(false))
			.andExpect(jsonPath("$.data.last").value(false))
			.andExpect(jsonPath("$.data.pageable").doesNotExist());
	}

	@Test
	@DisplayName("Slice를 전체 개수 없이 다음 페이지 여부를 포함한 계약으로 변환한다")
	void convertsSliceWithoutTotalCountMetadata() throws Exception {
		mockMvc.perform(get("/test/slice"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data", aMapWithSize(6)))
			.andExpect(jsonPath("$.data.content[0]").value("gamma"))
			.andExpect(jsonPath("$.data.content[1]").value("delta"))
			.andExpect(jsonPath("$.data.page").value(1))
			.andExpect(jsonPath("$.data.size").value(2))
			.andExpect(jsonPath("$.data.first").value(false))
			.andExpect(jsonPath("$.data.last").value(false))
			.andExpect(jsonPath("$.data.hasNext").value(true))
			.andExpect(jsonPath("$.data.totalElements").doesNotExist())
			.andExpect(jsonPath("$.data.totalPages").doesNotExist())
			.andExpect(jsonPath("$.data.pageable").doesNotExist());
	}

	@Test
	@DisplayName("페이징 응답의 content 스냅샷은 변경할 수 없다")
	void preservesImmutableContentSnapshots() {
		PageResponse<String> pageResponse = PageResponse.from(new PageImpl<>(List.of("alpha")));
		SliceResponse<String> sliceResponse = SliceResponse.from(new SliceImpl<>(List.of("beta")));

		assertThrows(UnsupportedOperationException.class, () -> pageResponse.getContent().add("changed"));
		assertThrows(UnsupportedOperationException.class, () -> sliceResponse.getContent().add("changed"));
	}

	@RestController
	static class TestController {

		@GetMapping("/test/success")
		ApiResponse<TestPayload> success() {
			return ApiResponse.of(new TestPayload(1));
		}

		@GetMapping("/test/page")
		ApiResponse<PageResponse<String>> page() {
			PageImpl<String> page = new PageImpl<>(List.of("alpha", "beta"), PageRequest.of(2, 2), 7);
			return ApiResponse.of(PageResponse.from(page));
		}

		@GetMapping("/test/slice")
		ApiResponse<SliceResponse<String>> slice() {
			SliceImpl<String> slice = new SliceImpl<>(List.of("gamma", "delta"), PageRequest.of(1, 2), true);
			return ApiResponse.of(SliceResponse.from(slice));
		}
	}

	static class TestPayload {

		private final long id;

		TestPayload(long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}
	}
}
