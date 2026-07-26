package com.chaean.promptdrive.member.internal.web;

import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OAuthOriginValidator {

	private final MemberOAuthProperties properties;

	public OAuthOriginValidator(MemberOAuthProperties properties) {
		this.properties = properties;
	}

	public void requireAllowedOrigin(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		if (origin == null || !properties.getAllowedOrigins().contains(origin)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Untrusted request origin");
		}
	}
}
