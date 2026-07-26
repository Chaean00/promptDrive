package com.chaean.promptdrive.member.internal.adapter.oauth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MemberOAuthProperties.class)
public class OAuthAdapterConfiguration {

	@Bean
	@Lazy
	public JwtDecoder googleIdTokenDecoder() {
		return JwtDecoders.fromIssuerLocation("https://accounts.google.com");
	}
}
