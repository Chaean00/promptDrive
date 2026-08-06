package com.chaean.promptdrive.prompt.internal.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.chaean.promptdrive.prompt.internal.persistence.projection.PromptRankingProjection;
import com.chaean.promptdrive.prompt.internal.persistence.projection.PromptSitemapProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;

public interface PromptRepository extends JpaRepository<Prompt, Long> {

	@Query("""
			SELECT p
			FROM Prompt p
			WHERE p.visibility = :visibility
			AND (:provenance IS NULL OR p.provenance = :provenance)
			AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
			AND (:category IS NULL OR exists (
				SELECT pc.id
				FROM PromptCategory pc
				WHERE pc.prompt = p
				AND pc.category = :category
			))
			ORDER BY p.createdAt DESC, p.title ASC
			""")
	Page<Prompt> findPublicPrompts(@Param("visibility") PromptVisibility visibility,
			@Param("provenance") PromptProvenance provenance,
			@Param("category") PromptCategoryType category,
			@Param("keyword") String keyword, Pageable pageable);

	default Page<Prompt> findPublicPrompts(PromptProvenance provenance, PromptCategoryType category, Pageable pageable) {
		return findPublicPrompts(provenance, category, null, pageable);
	}

	default Page<Prompt> findPublicPrompts(PromptProvenance provenance, PromptCategoryType category,
		String keyword, Pageable pageable) {
		return findPublicPrompts(PromptVisibility.PUBLIC, provenance, category, keyword, pageable);
	}

	Optional<Prompt> findByIdAndVisibility(Long id, PromptVisibility visibility);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Prompt p WHERE p.id = :id AND p.visibility = :visibility")
	Optional<Prompt> findByIdAndVisibilityForUpdate(@Param("id") Long id, @Param("visibility") PromptVisibility visibility);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
		UPDATE prompt
		SET copy_count = copy_count + 1
		WHERE id = :promptId
		AND visibility = 'PUBLIC'
		AND deleted_at IS NULL
		""", nativeQuery = true)
	int incrementPublicCopyCount(@Param("promptId") Long promptId);

	Optional<Prompt> findByIdAndProvenance(Long id, PromptProvenance provenance);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Prompt p WHERE p.id = :id AND p.provenance = :provenance")
	Optional<Prompt> findByIdAndProvenanceForUpdate(@Param("id") Long id, @Param("provenance") PromptProvenance provenance);

	Optional<Prompt> findByIdAndOwnerMemberIdAndProvenance(Long id, Long ownerMemberId, PromptProvenance provenance);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Prompt p WHERE p.id = :id AND p.ownerMemberId = :ownerMemberId AND p.provenance = :provenance")
	Optional<Prompt> findByIdAndOwnerMemberIdAndProvenanceForUpdate(@Param("id") Long id,
			@Param("ownerMemberId") Long ownerMemberId, @Param("provenance") PromptProvenance provenance);

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
		SELECT p.id AS id, p.updatedAt AS updatedAt
		FROM Prompt p
		WHERE p.visibility = PromptVisibility.PUBLIC
		ORDER BY p.id ASC
		""")
	List<PromptSitemapProjection> findPublicPromptSitemapEntries();

	@Query("""
		SELECT p AS prompt, COUNT(pl.id) AS likeCount
		FROM Prompt p
		LEFT JOIN PromptLike pl ON pl.prompt = p AND pl.deletedAt IS NULL
		AND (:likeCreatedAtLowerBound IS NULL OR pl.createdAt >= :likeCreatedAtLowerBound)
		WHERE p.visibility = :visibility
		AND p.deletedAt IS NULL
		GROUP BY p
		ORDER BY COUNT(pl.id) DESC, p.createdAt DESC, p.id DESC
		""")
	Slice<PromptRankingProjection> findPromptRankings(@Param("visibility") PromptVisibility visibility,
		@Param("likeCreatedAtLowerBound") LocalDateTime likeCreatedAtLowerBound,
		Pageable pageable);

	default Slice<PromptRankingProjection> findPublicPromptRankings(LocalDateTime likeCreatedAtLowerBound, Pageable pageable) {
		return findPromptRankings(PromptVisibility.PUBLIC, likeCreatedAtLowerBound, pageable);
	}
}
