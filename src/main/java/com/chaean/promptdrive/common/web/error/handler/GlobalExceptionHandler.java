package com.chaean.promptdrive.common.web.error.handler;

import java.util.List;

import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.ErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
import com.chaean.promptdrive.common.web.error.response.ApiErrorResponse;
import com.chaean.promptdrive.common.web.error.response.FieldErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception,
			HttpServletRequest request) {
		return error(exception.getErrorCode(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException exception, HttpServletRequest request) {
		return validationError(exception.getBindingResult().getFieldErrors(), request);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiErrorResponse> handleBindException(BindException exception, HttpServletRequest request) {
		return validationError(exception.getBindingResult().getFieldErrors(), request);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception, HttpServletRequest request) {
		return error(CommonErrorCode.INVALID_REQUEST, request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(NoResourceFoundException exception,
			HttpServletRequest request) {
		return error(CommonErrorCode.RESOURCE_NOT_FOUND, request, List.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleHttpRequestMethodNotSupportedException(
			HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
		return error(CommonErrorCode.METHOD_NOT_ALLOWED, request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		log.error("Unhandled exception for {}", request.getRequestURI(), exception);
		return error(CommonErrorCode.INTERNAL_SERVER_ERROR, request, List.of());
	}

	private ResponseEntity<ApiErrorResponse> validationError(
			List<org.springframework.validation.FieldError> fieldErrors, HttpServletRequest request) {
		List<FieldErrorResponse> errors = fieldErrors.stream()
			.map(fieldError -> FieldErrorResponse.of(fieldError.getField(), fieldError.getDefaultMessage()))
			.toList();
		return error(CommonErrorCode.VALIDATION_ERROR, request, errors);
	}

	private ResponseEntity<ApiErrorResponse> error(ErrorCode errorCode, HttpServletRequest request,
			List<FieldErrorResponse> fieldErrors) {
		ApiErrorResponse response = ApiErrorResponse.of(
			errorCode.getStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			request.getRequestURI(),
			fieldErrors
		);
		return ResponseEntity.status(errorCode.getStatus()).body(response);
	}
}
