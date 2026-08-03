package com.chaean.promptdrive.prompt.internal.web.seo;

import com.chaean.promptdrive.prompt.internal.application.seo.PromptSitemapService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PromptSeoController {

	private final PromptSitemapService promptSitemapService;

	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public String getSitemap() {
		return promptSitemapService.createSitemapXml();
	}

	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getRobots() {
		return promptSitemapService.createRobotsTxt();
	}
}
