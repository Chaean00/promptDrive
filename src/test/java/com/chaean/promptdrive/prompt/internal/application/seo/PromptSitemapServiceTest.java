package com.chaean.promptdrive.prompt.internal.application.seo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.chaean.promptdrive.common.config.SeoProperties;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionRepository;
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
	private PromptCollectionRepository collectionRepository;
	private PromptSitemapService service;
	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void setUp() {
		promptRepository = mock(PromptRepository.class);
		collectionRepository = mock(PromptCollectionRepository.class);
		SeoProperties properties = new SeoProperties();
		properties.setSiteUrl("https://prompt.example/search?a=1&b=2");
		CacheManager cacheManager = new CaffeineCacheManager("sitemap");
		((CaffeineCacheManager) cacheManager).setCaffeine(Caffeine.newBuilder().maximumSize(1));
		context = new AnnotationConfigApplicationContext();
		context.register(CacheConfig.class);
		context.registerBean(PromptRepository.class, () -> promptRepository);
		context.registerBean(PromptCollectionRepository.class, () -> collectionRepository);
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
		when(collectionRepository.findAll()).thenReturn(List.of());

		String first = service.createSitemapXml();
		String second = service.createSitemapXml();

		assertThat(second).isSameAs(first);
		verify(promptRepository, times(1)).findPublicPromptSitemapEntries();

	}

	@Test
	void onlyGeneratesSitemapOnceForConcurrentCacheMisses() throws Exception {
		AtomicInteger repositoryCalls = new AtomicInteger();
		CountDownLatch concurrentCalls = new CountDownLatch(2);
		when(promptRepository.findPublicPromptSitemapEntries()).thenAnswer(invocation -> {
			repositoryCalls.incrementAndGet();
			concurrentCalls.countDown();
			concurrentCalls.await(1, TimeUnit.SECONDS);
			return List.of();
		});
		when(collectionRepository.findAll()).thenReturn(List.of());

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<String> first = executor.submit(service::createSitemapXml);
			Future<String> second = executor.submit(service::createSitemapXml);

			assertThat(first.get(3, TimeUnit.SECONDS)).isEqualTo(second.get(3, TimeUnit.SECONDS));
			assertThat(repositoryCalls).hasValue(1);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void escapesGeneratedUrlValuesAndKeepsRobotsPlainTextContract() {
		when(promptRepository.findPublicPromptSitemapEntries()).thenReturn(List.of());
		when(collectionRepository.findAll()).thenReturn(List.of());

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
