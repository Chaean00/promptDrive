package com.chaean.promptdrive.common.web.error.response;

import lombok.Getter;

@Getter
public class FieldErrorResponse {

	private final String field;
	private final String message;

	private FieldErrorResponse(String field, String message) {
		this.field = field;
		this.message = message;
	}

	public static FieldErrorResponse of(String field, String message) {
		return new FieldErrorResponse(field, message);
	}
}
