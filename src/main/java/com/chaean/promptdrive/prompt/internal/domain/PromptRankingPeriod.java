package com.chaean.promptdrive.prompt.internal.domain;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromptRankingPeriod {

	SEVEN_DAYS("7d", "7 Days", "7일"),
	THIRTY_DAYS("30d", "30 Days", "30일"),
	ALL("all", "All", "전체");

	private final String code;
	private final String englishName;
	private final String koreanName;

	public static Optional<PromptRankingPeriod> fromCode(String code) {
		return Arrays.stream(values()).filter(period -> period.code.equals(code)).findFirst();
	}

	public LocalDateTime resolveLikeCreatedAtLowerBound(LocalDateTime now) {
		return switch (this) {
			case SEVEN_DAYS -> now.minusDays(7);
			case THIRTY_DAYS -> now.minusDays(30);
			case ALL -> null;
		};
	}
}
