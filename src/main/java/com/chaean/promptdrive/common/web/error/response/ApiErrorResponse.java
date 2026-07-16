package com.chaean.promptdrive.common.web.error.response;

import java.util.List;

import lombok.Getter;

@Getter
public class ApiErrorResponse {

	private final int status;
	private final String code;
	private final String message;
	private final String path;
	private final List<FieldErrorResponse> fieldErrors;

	private ApiErrorResponse(int status, String code, String message, String path,
			List<FieldErrorResponse> fieldErrors) {
		this.status = status;
		this.code = code;
		this.message = message;
		this.path = path;
		this.fieldErrors = List.copyOf(fieldErrors);
	}

	public static ApiErrorResponse of(int status, String code, String message, String path,
			List<FieldErrorResponse> fieldErrors) {
		return new ApiErrorResponse(status, code, message, path, fieldErrors);
	}
}
