package com.chaean.promptdrive.common.web.error.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.chaean.promptdrive.common.web.error.ErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsValidationErrorWithFieldErrors() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("입력 값을 확인해주세요."))
			.andExpect(jsonPath("$.path").value("/test/validation"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("제목은 필수입니다."));
	}

	@Test
	void returnsDomainErrorFromBusinessException() throws Exception {
		mockMvc.perform(get("/test/business"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("PROMPT_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("프롬프트를 찾을 수 없습니다."))
			.andExpect(jsonPath("$.path").value("/test/business"))
			.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void returnsBadRequestForMalformedJson() throws Exception {
		mockMvc.perform(post("/test/json")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"count\":"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
	}

	@Test
	void returnsBadRequestForTypeMismatch() throws Exception {
		mockMvc.perform(get("/test/type").param("count", "not-a-number"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
	}

	@Test
	void returnsInternalServerErrorWithoutExceptionDetails() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("COMMON_INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("sensitive detail")));
	}

	@Test
	void returnsNotFoundForMissingResource() throws Exception {
		mockMvc.perform(get("/missing-resource"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"));
	}

	@Test
	void returnsMethodNotAllowedForUnsupportedMethod() throws Exception {
		mockMvc.perform(get("/test/validation"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("COMMON_METHOD_NOT_ALLOWED"));
	}

	@Controller
	@RequestMapping("/test")
	static class TestController {

		@PostMapping("/validation")
		@ResponseBody
		void validate(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/business")
		@ResponseBody
		void businessException() {
			throw new BusinessException(TestErrorCode.PROMPT_NOT_FOUND);
		}

		@PostMapping("/json")
		@ResponseBody
		void json(@RequestBody JsonRequest request) {
		}

		@GetMapping("/type")
		@ResponseBody
		void type(@RequestParam Integer count) {
		}

		@GetMapping("/unexpected")
		@ResponseBody
		void unexpected() {
			throw new IllegalStateException("sensitive detail");
		}
	}

	private enum TestErrorCode implements ErrorCode {

		PROMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT_NOT_FOUND", "프롬프트를 찾을 수 없습니다.");

		private final HttpStatus status;
		private final String code;
		private final String message;

		TestErrorCode(HttpStatus status, String code, String message) {
			this.status = status;
			this.code = code;
			this.message = message;
		}

		@Override
		public HttpStatus getStatus() {
			return status;
		}

		@Override
		public String getCode() {
			return code;
		}

		@Override
		public String getMessage() {
			return message;
		}
	}

	static class ValidationRequest {

		@NotBlank(message = "제목은 필수입니다.")
		private String title;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	static class JsonRequest {

		private Integer count;

		public Integer getCount() {
			return count;
		}

		public void setCount(Integer count) {
			this.count = count;
		}
	}
}
