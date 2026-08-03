package com.chaean.promptdrive.prompt.internal.application.seo;

import java.time.format.DateTimeFormatter;

import com.chaean.promptdrive.common.config.SeoProperties;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.persistence.PromptRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptSitemapService {

	private static final DateTimeFormatter LAST_MODIFIED_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	private final PromptRepository promptRepository;
	private final SeoProperties seoProperties;

	public String createSitemapXml() {
		String siteUrl = normalizedSiteUrl();
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
		appendUrl(xml, siteUrl, null);
		for (PromptCategoryType category : PromptCategoryType.values()) {
			appendUrl(xml, siteUrl + "/categories/" + category.getCode(), null);
		}
		promptRepository.findPublicPromptSitemapEntries().forEach(prompt ->
			appendUrl(xml, siteUrl + "/prompts/" + prompt.getId(), prompt.getUpdatedAt().format(LAST_MODIFIED_FORMATTER)));
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
		xml.append("<url><loc>").append(location).append("</loc>");
		if (lastModified != null) {
			xml.append("<lastmod>").append(lastModified).append("</lastmod>");
		}
		xml.append("</url>");
	}
}
