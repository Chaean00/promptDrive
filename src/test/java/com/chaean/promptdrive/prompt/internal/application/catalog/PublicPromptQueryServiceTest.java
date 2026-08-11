package com.chaean.promptdrive.prompt.internal.application.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class PublicPromptQueryServiceTest {

	@Test
	void keywordSearchEntryPointHasReadOnlyTransactionContract() throws NoSuchMethodException {
		Transactional transactional = PublicPromptQueryService.class
			.getMethod("getPublicPromptPage", PromptProvenance.class, PromptCategoryType.class, String.class,
				Integer.class, Integer.class)
			.getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isTrue();
	}
}
