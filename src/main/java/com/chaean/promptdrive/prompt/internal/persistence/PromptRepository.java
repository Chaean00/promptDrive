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

	Optional<Prompt> findByIdAndOwnerMemberIdAndProvenance(Long id, Long ownerMemberId, PromptProvenance provenance);

	Slice<Prompt> findAllByOwnerMemberIdAndProvenanceOrderByCreatedAtDescIdDesc(Long ownerMemberId,
		PromptProvenance provenance, Pageable pageable);

	@Query("""
		SELECT p
		FROM Prompt p
		WHERE p.provenance = PromptProvenance.CURATED
		AND (:visibility IS NULL OR p.visibility = :visibility)
		ORDER BY p.createdAt DESC, p.id DESC
		""")
	Slice<Prompt> findCuratedPrompts(@Param("visibility") PromptVisibility visibility, Pageable pageable);

	@Query("""
		SELECT p AS prompt, COUNT(pl.id) AS likeCount
		FROM Prompt p
		LEFT JOIN PromptLike pl ON pl.prompt = p AND pl.deletedAt IS NULL
		WHERE p.visibility = :visibility
		AND p.deletedAt IS NULL
		GROUP BY p
		ORDER BY COUNT(pl.id) DESC, p.createdAt DESC, p.id DESC
		""")
	Slice<PromptRankingProjection> findPromptRankings(@Param("visibility") PromptVisibility visibility,
		Pageable pageable);

	default Slice<PromptRankingProjection> findPublicPromptRankings(Pageable pageable) {
		return findPromptRankings(PromptVisibility.PUBLIC, pageable);
	}
}
