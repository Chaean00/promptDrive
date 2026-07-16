package com.chaean.promptdrive.common.web.error.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.chaean.promptdrive.common.web.error.ErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.trace.TraceIdContext;
import com.chaean.promptdrive.common.web.trace.TraceIdFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class, GlobalExceptionHandlerTest.TestController.class})
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("전역 예외 처리")
class GlobalExceptionHandlerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private TraceIdFilter traceIdFilter;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.addFilters(traceIdFilter)
			.build();
	}

	@Nested
	@DisplayName("오류 응답")
	class ErrorResponse {

		@Test
		@DisplayName("검증 실패 시 필드 오류를 포함한 400 응답을 반환한다")
		void returnsValidationErrorWithFieldErrors() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("입력 값을 확인해주세요."))
			.andExpect(jsonPath("$.path").value("/test/validation"))
			.andExpect(jsonPath("$.traceId", org.hamcrest.Matchers.matchesPattern(
				"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("제목은 필수입니다."));
		}

		@Test
		@DisplayName("도메인 예외를 정의된 오류 응답으로 반환한다")
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
		@DisplayName("잘못된 JSON 요청을 400 응답으로 반환한다")
		void returnsBadRequestForMalformedJson() throws Exception {
		mockMvc.perform(post("/test/json")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"count\":"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
		}

		@Test
		@DisplayName("요청 파라미터 타입 불일치를 400 응답으로 반환한다")
		void returnsBadRequestForTypeMismatch() throws Exception {
		mockMvc.perform(get("/test/type").param("count", "not-a-number"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
		}

		@Test
		@DisplayName("예상하지 못한 예외에서 민감정보 없는 500 응답을 반환한다")
		void returnsInternalServerErrorWithoutExceptionDetails() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("COMMON_INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("sensitive detail")));
		}

		@Test
		@DisplayName("없는 리소스 요청을 404 응답으로 반환한다")
		void returnsNotFoundForMissingResource() throws Exception {
		mockMvc.perform(get("/missing-resource"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"));
		}

		@Test
		@DisplayName("지원하지 않는 HTTP 메서드를 405 응답으로 반환한다")
		void returnsMethodNotAllowedForUnsupportedMethod() throws Exception {
		mockMvc.perform(get("/test/validation"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("COMMON_METHOD_NOT_ALLOWED"));
		}
	}

	@Nested
	@DisplayName("Trace ID 생성 및 전달")
	class TraceIdPropagation {

		@Test
		@DisplayName("클라이언트 Trace ID를 무시하고 서버 생성값을 응답에 사용한 뒤 MDC를 정리한다")
		void ignoresClientTraceIdAndClearsMdcAfterCompletion() throws Exception {
			String clientTraceId = "client-trace-id";

			mockMvc.perform(get("/test/business").header(TraceIdContext.HEADER_NAME, clientTraceId))
				.andExpect(status().isNotFound())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
					.string(TraceIdContext.HEADER_NAME,
						org.hamcrest.Matchers.matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
				.andExpect(jsonPath("$.traceId", org.hamcrest.Matchers.matchesPattern(
					"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
				.andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.not(clientTraceId)));

			org.junit.jupiter.api.Assertions.assertNull(MDC.get(TraceIdContext.MDC_KEY));
		}

		@Test
		@DisplayName("동일 요청 스레드의 로그에 Trace ID를 자동으로 출력한다")
		void makesTraceIdAvailableToLogsInTheRequestThread(CapturedOutput output) throws Exception {
			org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/test/trace"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
					.string(TraceIdContext.HEADER_NAME,
						org.hamcrest.Matchers.matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
				.andReturn();
			String traceId = result.getResponse().getHeader(TraceIdContext.HEADER_NAME);

			org.junit.jupiter.api.Assertions.assertEquals(traceId, result.getResponse().getContentAsString());
			org.junit.jupiter.api.Assertions.assertTrue(output.getOut().contains("[traceId=" + traceId + "]"));
			org.junit.jupiter.api.Assertions.assertTrue(output.getOut()
				.contains("HTTP request completed: method=GET, path=/test/trace, status=200"));
		}

		@Test
		@DisplayName("모든 요청에 서버 생성 UUID를 할당한다")
		void generatesTraceIdForEveryRequest() throws Exception {
			mockMvc.perform(get("/test/business"))
				.andExpect(status().isNotFound())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
					.string(TraceIdContext.HEADER_NAME,
						org.hamcrest.Matchers.matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
				.andExpect(jsonPath("$.traceId", org.hamcrest.Matchers.matchesPattern(
					"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
		}
	}

	@Controller
	@RequestMapping("/test")
	static class TestController {

		private static final Logger log = LoggerFactory.getLogger(TestController.class);

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

		@GetMapping("/trace")
		@ResponseBody
		String traceId() {
			log.info("Trace ID is available in the request thread");
			return MDC.get(TraceIdContext.MDC_KEY);
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
