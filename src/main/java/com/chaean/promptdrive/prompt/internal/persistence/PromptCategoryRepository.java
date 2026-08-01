package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptCategoryRepository extends JpaRepository<PromptCategory, Long> {

	List<PromptCategory> findAllByPromptIdIn(Collection<Long> promptIds);

	List<PromptCategory> findAllByPromptId(Long promptId);
}
