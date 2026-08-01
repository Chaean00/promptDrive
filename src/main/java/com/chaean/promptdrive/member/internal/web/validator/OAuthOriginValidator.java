package com.chaean.promptdrive.member.internal.web.validator;

import com.chaean.promptdrive.member.internal.adapter.oauth.MemberOAuthProperties;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthOriginValidator {

	private final MemberOAuthProperties properties;

	public void requireAllowedOrigin(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		if (origin == null || !properties.getAllowedOrigins().contains(origin)) {
			throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
		}
	}
}
