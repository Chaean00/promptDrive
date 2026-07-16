package com.chaean.promptdrive.common.web.trace;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;

public final class TraceIdContext {

	public static final String HEADER_NAME = "X-Trace-Id";
	public static final String ATTRIBUTE_NAME = TraceIdContext.class.getName() + ".traceId";
	public static final String MDC_KEY = "traceId";

	private TraceIdContext() {
	}

	public static String getTraceId(HttpServletRequest request) {
		Object traceId = request.getAttribute(ATTRIBUTE_NAME);
		if (traceId instanceof String value) {
			return value;
		}
		return MDC.get(MDC_KEY);
	}
}
