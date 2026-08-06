package com.chaean.promptdrive.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
@Validated
public class JwtProperties {

	@NotBlank
	private String issuer;
	@NotBlank
	private String audience;
	@NotBlank
	private String signingKey;
	private Duration accessTokenTtl = Duration.ofMinutes(15);
	private Duration refreshTokenTtl = Duration.ofDays(30);
	private boolean refreshCookieSecure = true;
}
