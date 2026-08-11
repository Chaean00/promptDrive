package com.chaean.promptdrive.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "seo")
public class SeoProperties {

	private String siteUrl;
	private long sitemapCacheMaximumSize = 1;
	private Duration sitemapCacheTtl = Duration.ofMinutes(5);
}
