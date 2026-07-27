package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;

public interface PromptRepository extends JpaRepository<Prompt, Long> {

	@Query("""
			SELECT p
			FROM Prompt p
			WHERE p.visibility = :visibility
			AND (:provenance IS NULL OR p.provenance = :provenance)
			AND (:category IS NULL OR exists (
				SELECT pc.id
				FROM PromptCategory pc
				WHERE pc.prompt = p
				AND pc.category = :category
			))
			ORDER BY p.createdAt DESC, p.id DESC
			""")
	Slice<Prompt> findPublicPrompts(@Param("visibility") PromptVisibility visibility,
			@Param("provenance") PromptProvenance provenance,
			@Param("category") PromptCategoryType category, Pageable pageable);

	default Slice<Prompt> findPublicPrompts(PromptProvenance provenance, PromptCategoryType category, Pageable pageable) {
		return findPublicPrompts(PromptVisibility.PUBLIC, provenance, category, pageable);
	}

	Optional<Prompt> findByIdAndVisibility(Long id, PromptVisibility visibility);

	Optional<Prompt> findByIdAndProvenance(Long id, PromptProvenance provenance);

	@Query("""
		SELECT p
		FROM Prompt p
		WHERE p.provenance = com.chaean.promptdrive.prompt.internal.domain.PromptProvenance.CURATED
		AND (:visibility IS NULL OR p.visibility = :visibility)
		ORDER BY p.createdAt DESC, p.id DESC
		""")
	Slice<Prompt> findCuratedPrompts(@Param("visibility") PromptVisibility visibility, Pageable pageable);
}
