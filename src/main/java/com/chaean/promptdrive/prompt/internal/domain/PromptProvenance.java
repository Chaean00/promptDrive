package com.chaean.promptdrive.prompt.internal.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromptProvenance {

	CURATED("Curated", "운영 수집"),
	COMMUNITY("Community", "커뮤니티");

	private final String englishName;
	private final String koreanName;

	public String getCode() {
		return name();
	}
}
