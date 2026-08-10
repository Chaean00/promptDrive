package com.chaean.promptdrive.prompt.internal.application.seo;

import java.time.format.DateTimeFormatter;

import com.chaean.promptdrive.common.config.SeoProperties;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;
import com.chaean.promptdrive.prompt.internal.persistence.PromptCollectionRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptSitemapService {

	private static final DateTimeFormatter LAST_MODIFIED_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	private final PromptRepository promptRepository;
	private final PromptCollectionRepository collectionRepository;
	private final SeoProperties seoProperties;

	@Cacheable(cacheNames = "sitemap")
	public String createSitemapXml() {
		return generateSitemapXml();
	}

	private String generateSitemapXml() {
		String siteUrl = normalizedSiteUrl();
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
		appendUrl(xml, siteUrl, null);
		for (PromptCategoryType category : PromptCategoryType.values()) {
			appendUrl(xml, siteUrl + "/categories/" + category.getCode(), null);
		}
		promptRepository.findPublicPromptSitemapEntries().forEach(prompt ->
			appendUrl(xml, siteUrl + "/prompts/" + prompt.getId(), prompt.getUpdatedAt().format(LAST_MODIFIED_FORMATTER)));
		collectionRepository.findAll().forEach(collection ->
			appendUrl(xml, siteUrl + "/prompt-collections/" + collection.getSlug(), collection.getUpdatedAt().format(LAST_MODIFIED_FORMATTER)));
		xml.append("</urlset>");
		return xml.toString();
	}

	public String createRobotsTxt() {
		return "User-agent: *\nAllow: /\nSitemap: " + normalizedSiteUrl() + "/sitemap.xml\n";
	}

	private String normalizedSiteUrl() {
		String siteUrl = seoProperties.getSiteUrl();
		if (siteUrl == null || siteUrl.isBlank()) {
			throw new IllegalStateException("seo.site-url must be configured");
		}
		return siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
	}

	private void appendUrl(StringBuilder xml, String location, String lastModified) {
		xml.append("<url><loc>").append(HtmlUtils.htmlEscape(location)).append("</loc>");
		if (lastModified != null) {
			xml.append("<lastmod>").append(lastModified).append("</lastmod>");
		}
		xml.append("</url>");
	}
}
