package com.chaean.promptdrive.common.web.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum CommonErrorCode implements ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "COMMON_VALIDATION_ERROR", "입력 값을 확인해주세요."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_INVALID_REQUEST", "잘못된 요청입니다."),
	UNAUTHORIZED_REQUEST(HttpStatus.UNAUTHORIZED, "COMMON_UNAUTHORIZED_REQUEST", "인증 요청이 유효하지 않습니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_ACCESS_DENIED", "접근이 거부되었습니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON_SERVICE_UNAVAILABLE", "현재 서비스를 사용할 수 없습니다."),
	EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "COMMON_EXTERNAL_SERVICE_ERROR", "외부 서비스 처리 중 오류가 발생했습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
	INVALID_SECURITY_CONFIGURATION(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INVALID_SECURITY_CONFIGURATION", "보안 설정이 올바르지 않습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	CommonErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
