package com.chaean.promptdrive.prompt.internal.application.seo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.chaean.promptdrive.common.config.SeoProperties;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;
import com.chaean.promptdrive.prompt.internal.persistence.projection.PromptSitemapProjection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

class PromptSitemapServiceTest {

	private PromptRepository promptRepository;
	private PromptSitemapService service;
	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void setUp() {
		promptRepository = mock(PromptRepository.class);
		SeoProperties properties = new SeoProperties();
		properties.setSiteUrl("https://prompt.example/search?a=1&b=2");
		CacheManager cacheManager = new CaffeineCacheManager("sitemap");
		((CaffeineCacheManager) cacheManager).setCaffeine(Caffeine.newBuilder().maximumSize(1));
		context = new AnnotationConfigApplicationContext();
		context.register(CacheConfig.class);
		context.registerBean(PromptRepository.class, () -> promptRepository);
		context.registerBean(SeoProperties.class, () -> properties);
		context.registerBean(CacheManager.class, () -> cacheManager);
		context.registerBean(PromptSitemapService.class);
		context.refresh();
		service = context.getBean(PromptSitemapService.class);
	}

	@AfterEach
	void tearDown() {
		context.close();
	}

	@Test
	void cachesSitemapForRepeatedRequests() {
		PromptSitemapProjection prompt = mock(PromptSitemapProjection.class);
		when(prompt.getId()).thenReturn(42L);
		when(prompt.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 8, 10, 12, 0));
		when(promptRepository.findPublicPromptSitemapEntries()).thenReturn(List.of(prompt));

		String first = service.createSitemapXml();
		String second = service.createSitemapXml();

		assertThat(second).isSameAs(first);
		verify(promptRepository, times(1)).findPublicPromptSitemapEntries();

	}

	@Test
	void escapesGeneratedUrlValuesAndKeepsRobotsPlainTextContract() {
		when(promptRepository.findPublicPromptSitemapEntries()).thenReturn(List.of());

		assertThat(service.createSitemapXml())
			.contains("https://prompt.example/search?a=1&amp;b=2")
			.doesNotContain("<loc>https://prompt.example/search?a=1&b=2</loc>");
		assertThat(service.createRobotsTxt())
			.isEqualTo("User-agent: *\nAllow: /\nSitemap: https://prompt.example/search?a=1&b=2/sitemap.xml\n");
	}

	@Configuration
	@EnableCaching
	static class CacheConfig {
	}
}
