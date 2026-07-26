package com.chaean.promptdrive.member.internal.adapter.oauth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MemberOAuthProperties.class)
public class OAuthAdapterConfiguration {
}
