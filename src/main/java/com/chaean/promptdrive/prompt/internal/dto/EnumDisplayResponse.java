package com.chaean.promptdrive.prompt.internal.dto;

import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EnumDisplayResponse {

	private final String code;
	private final String englishName;
	private final String koreanName;

	public static EnumDisplayResponse from(PromptCategoryType value) {
		return new EnumDisplayResponse(value.getCode(), value.getEnglishName(), value.getKoreanName());
	}

	public static EnumDisplayResponse from(PromptProvenance value) {
		return new EnumDisplayResponse(value.getCode(), value.getEnglishName(), value.getKoreanName());
	}

	public static EnumDisplayResponse from(PromptVisibility value) {
		return new EnumDisplayResponse(value.getCode(), value.getEnglishName(), value.getKoreanName());
	}
}
