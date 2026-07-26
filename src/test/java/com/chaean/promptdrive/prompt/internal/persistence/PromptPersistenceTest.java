package com.chaean.promptdrive.prompt.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;

class PromptPersistenceTest {

	@Test
	void curatedFactoryCreatesOwnerlessCuratedPrompt() {
		Prompt prompt = Prompt.createCurated("title", "content", PromptVisibility.PUBLIC, null, null);

		assertThat(prompt.getProvenance()).isEqualTo(PromptProvenance.CURATED);
		assertThat(prompt.getOwnerMemberId()).isNull();
	}

	@Test
	void curatedPromptRejectsOwner() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Prompt(
				"title", "content", PromptProvenance.CURATED, PromptVisibility.PUBLIC, 1L, null, null));
	}

	@Test
	void curatedLifecycleUpdatesVisibilityAndSoftDeletes() {
		Prompt prompt = Prompt.createCurated("title", "content", PromptVisibility.PUBLIC, null, null);

		prompt.updateCurated("updated", "new content", "source", "https://example.com");
		prompt.changeVisibility(PromptVisibility.HIDDEN);
		prompt.deleteCurated();

		assertThat(prompt.getTitle()).isEqualTo("updated");
		assertThat(prompt.getVisibility()).isEqualTo(PromptVisibility.HIDDEN);
		assertThat(prompt.getDeletedAt()).isNotNull();
	}

	@Test
	void communityPromptCannotUseCuratedLifecycle() {
		Prompt prompt = new Prompt("title", "content", PromptProvenance.COMMUNITY,
				PromptVisibility.PUBLIC, 1L, null, null);

		assertThatIllegalStateException().isThrownBy(() -> prompt.changeVisibility(PromptVisibility.HIDDEN));
	}

	@Test
	void categoryRequiresPromptAndControlledEnum() {
		Prompt prompt = Prompt.createCurated("title", "content", PromptVisibility.PUBLIC, null, null);
		PromptCategory category = PromptCategory.create(prompt, PromptCategoryType.DEVELOPMENT);

		assertThat(category.getPrompt()).isSameAs(prompt);
		assertThat(category.getCategory()).isEqualTo(PromptCategoryType.DEVELOPMENT);
		assertThatNullPointerException().isThrownBy(() -> new PromptCategory(prompt, null));
		assertThatNullPointerException().isThrownBy(() -> new PromptCategory(null, PromptCategoryType.DEVELOPMENT));
	}
}
