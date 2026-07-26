package com.chaean.promptdrive.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

class JwtSigningKeyConditionTest {

	private final JwtSigningKeyCondition condition = new JwtSigningKeyCondition();
	private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

	@Test
	void doesNotMatchWhenSigningKeyIsMissingOrBlank() {
		assertThat(matches(new MockEnvironment())).isFalse();
		assertThat(matches(new MockEnvironment().withProperty("security.jwt.signing-key", "  "))).isFalse();
	}

	@Test
	void matchesWhenSigningKeyIsConfigured() {
		assertThat(matches(new MockEnvironment().withProperty("security.jwt.signing-key", "encoded-key"))).isTrue();
	}

	private boolean matches(MockEnvironment environment) {
		ConditionContext context = mock(ConditionContext.class);
		given(context.getEnvironment()).willReturn(environment);
		return condition.matches(context, metadata);
	}
}
