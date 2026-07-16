package com.chaean.promptdrive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

@DisplayName("애플리케이션 모듈 구조")
class PromptDriveApplicationTests {

	private final ApplicationModules modules = ApplicationModules.of(PromptDriveApplication.class);

	@Test
	@DisplayName("모듈 의존성 규칙을 만족한다")
	void verifiesModulithBoundaries() {
		modules.verify();
	}

}
