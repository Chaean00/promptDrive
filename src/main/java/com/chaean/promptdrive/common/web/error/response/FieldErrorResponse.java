package com.chaean.promptdrive.common.web.error.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldErrorResponse {

	private final String field;
	private final String message;

	public static FieldErrorResponse of(String field, String message) {
		return new FieldErrorResponse(field, message);
	}
}
