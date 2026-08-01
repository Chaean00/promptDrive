package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptLikeRepository extends JpaRepository<PromptLike, Long> {

	Optional<PromptLike> findByPromptIdAndMemberId(Long promptId, Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		UPDATE IGNORE prompt_like
		SET deleted_at = NULL
		WHERE prompt_id = :promptId
		AND member_id = :memberId
		AND deleted_at IS NOT NULL
		ORDER BY id DESC
		LIMIT 1
		""", nativeQuery = true)
	int reactivateLatestDeletedByPromptIdAndMemberId(@Param("promptId") Long promptId,
		@Param("memberId") Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		INSERT INTO prompt_like (prompt_id, member_id)
		VALUES (:promptId, :memberId)
		ON DUPLICATE KEY UPDATE id = id
		""", nativeQuery = true)
	int insertIfAbsent(@Param("promptId") Long promptId, @Param("memberId") Long memberId);

}
