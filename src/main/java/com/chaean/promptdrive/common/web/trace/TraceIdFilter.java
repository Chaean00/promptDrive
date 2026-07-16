package com.chaean.promptdrive.common.web.trace;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = UUID.randomUUID().toString();
		long startedAt = System.nanoTime();

		request.setAttribute(TraceIdContext.ATTRIBUTE_NAME, traceId);
		response.setHeader(TraceIdContext.HEADER_NAME, traceId);
		MDC.put(TraceIdContext.MDC_KEY, traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
			log.info("HTTP request completed: method={}, path={}, status={}, durationMs={}",
				request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
			MDC.remove(TraceIdContext.MDC_KEY);
		}
	}
}
