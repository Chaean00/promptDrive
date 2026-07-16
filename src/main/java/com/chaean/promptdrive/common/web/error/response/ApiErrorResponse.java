package com.chaean.promptdrive.common.web.error.response;

import java.util.List;

import lombok.Value;

@Value
public class ApiErrorResponse {

	int status;
	String code;
	String message;
	String path;
	List<FieldErrorResponse> fieldErrors;
}
