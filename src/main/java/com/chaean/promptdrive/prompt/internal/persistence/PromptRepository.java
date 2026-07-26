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
			select p from Prompt p
			where p.visibility = :visibility
			and (:provenance is null or p.provenance = :provenance)
			and (:category is null or exists (
				select pc.id from PromptCategory pc
				where pc.prompt = p and pc.category = :category
			))
			order by p.createdAt desc, p.id desc
			""")
	Slice<Prompt> findPublicPrompts(@Param("visibility") PromptVisibility visibility,
			@Param("provenance") PromptProvenance provenance,
			@Param("category") PromptCategoryType category, Pageable pageable);

	default Slice<Prompt> findPublicPrompts(PromptProvenance provenance, PromptCategoryType category, Pageable pageable) {
		return findPublicPrompts(PromptVisibility.PUBLIC, provenance, category, pageable);
	}

	Optional<Prompt> findByIdAndVisibility(Long id, PromptVisibility visibility);

	Optional<Prompt> findByIdAndProvenance(Long id, PromptProvenance provenance);

	Slice<Prompt> findByProvenance(PromptProvenance provenance, Pageable pageable);

	Slice<Prompt> findByProvenanceAndVisibility(PromptProvenance provenance, PromptVisibility visibility,
			Pageable pageable);
}
