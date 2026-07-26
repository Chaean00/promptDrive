package com.chaean.promptdrive.common.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class JwtSigningKeyCondition implements Condition {

	private static final String SIGNING_KEY_PROPERTY = "security.jwt.signing-key";

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String signingKey = context.getEnvironment().getProperty(SIGNING_KEY_PROPERTY);
		return signingKey != null && !signingKey.isBlank();
	}
}
