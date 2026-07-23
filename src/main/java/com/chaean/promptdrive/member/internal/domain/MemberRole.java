package com.chaean.promptdrive.member.internal.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

	MEMBER("Member", "회원"),
	ADMIN("Administrator", "관리자");

	private final String englishName;
	private final String koreanName;

	public String getCode() {
		return name();
	}
}
