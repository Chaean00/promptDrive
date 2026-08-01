package com.chaean.promptdrive.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

	private String issuer;
	private String audience;
	private String signingKey;
	private Duration accessTokenTtl = Duration.ofMinutes(15);
	private Duration refreshTokenTtl = Duration.ofDays(30);
	private boolean refreshCookieSecure = true;
}
