package com.chaean.promptdrive.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(SeoProperties.class)
public class SeoConfiguration {

	@Bean
	public CacheManager cacheManager(SeoProperties properties) {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager("sitemap");
		cacheManager.setCaffeine(Caffeine.newBuilder()
			.maximumSize(properties.getSitemapCacheMaximumSize())
			.expireAfterWrite(properties.getSitemapCacheTtl()));
		return cacheManager;
	}
}
