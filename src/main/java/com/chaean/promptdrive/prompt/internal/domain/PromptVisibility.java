package com.chaean.promptdrive.prompt.internal.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromptVisibility {

	PUBLIC("Public", "공개"),
	HIDDEN("Hidden", "비공개");

	private final String englishName;
	private final String koreanName;

	public String getCode() {
		return name();
	}
}
