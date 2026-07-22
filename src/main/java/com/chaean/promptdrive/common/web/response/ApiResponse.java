package com.chaean.promptdrive.common.web.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

	private final T data;

	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(data);
	}
}
