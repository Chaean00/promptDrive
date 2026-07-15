package com.chaean.promptdrive;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class PromptDriveApplicationTests {

	private final ApplicationModules modules = ApplicationModules.of(PromptDriveApplication.class);

	@Test
	void verifiesModulithBoundaries() {
		modules.verify();
	}

}
