package com.chaean.promptdrive.prompt.internal.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromptCategoryType {

	DEVELOPMENT("Development", "개발"),
	DEBUGGING("Debugging", "디버깅"),
	CODE_REVIEW("Code Review", "코드 리뷰"),
	TESTING("Testing", "테스트"),
	DATA_ANALYSIS("Data Analysis", "데이터 분석"),
	DATABASE("Database", "데이터베이스"),
	DEVOPS("DevOps", "데브옵스"),
	CLOUD("Cloud", "클라우드"),
	SECURITY("Security", "보안"),
	ARCHITECTURE("Software Architecture", "소프트웨어 아키텍처"),
	API_DESIGN("API Design", "API 설계"),
	TECHNICAL_WRITING("Technical Writing", "기술 글쓰기"),
	DOCUMENTATION("Documentation", "문서"),
	TRANSLATION("Translation", "번역"),
	WRITING("Writing", "글쓰기"),
	EDITING("Editing", "교정·편집"),
	SUMMARIZATION("Summarization", "요약"),
	EMAIL("Email", "이메일"),
	MEETING("Meeting", "회의"),
	PRESENTATION("Presentation", "발표"),
	RESUME("Resume", "이력서"),
	SELF_INTRODUCTION("Self Introduction", "자기소개서"),
	COVER_LETTER("Cover Letter", "지원서"),
	INTERVIEW("Interview", "면접"),
	CAREER("Career", "커리어"),
	LEARNING("Learning", "학습"),
	RESEARCH("Research", "리서치"),
	PRODUCTIVITY("Productivity", "생산성"),
	PROJECT_MANAGEMENT("Project Management", "프로젝트 관리"),
	BUSINESS("Business", "비즈니스"),
	MARKETING("Marketing", "마케팅"),
	SALES("Sales", "영업"),
	CUSTOMER_SUPPORT("Customer Support", "고객 지원"),
	DESIGN("Design", "디자인"),
	UX_UI("UX/UI", "UX/UI"),
	CONTENT_CREATION("Content Creation", "콘텐츠 제작"),
	FINANCE("Finance", "금융"),
	LEGAL("Legal", "법률"),
	HEALTHCARE("Healthcare", "헬스케어"),
	EDUCATION("Education", "교육");

	private final String englishName;
	private final String koreanName;

	public String getCode() {
		return name();
	}
}
