package com.chaean.promptdrive.common.web.error.response;

import lombok.Value;

@Value
public class FieldErrorResponse {

	String field;
	String message;
}
