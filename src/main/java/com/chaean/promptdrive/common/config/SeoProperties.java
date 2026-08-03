package com.chaean.promptdrive.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "seo")
public class SeoProperties {

	private String siteUrl;
}
