package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptBookmarkRepository extends JpaRepository<PromptBookmark, Long> {

	@Query("""
		SELECT bookmark.prompt
		FROM PromptBookmark bookmark
		WHERE bookmark.memberId = :memberId
		AND bookmark.prompt.visibility = com.chaean.promptdrive.prompt.internal.domain.PromptVisibility.PUBLIC
		ORDER BY bookmark.updatedAt DESC, bookmark.id DESC
		""")
	Slice<Prompt> findBookmarkedPublicPrompts(@Param("memberId") Long memberId, Pageable pageable);

	@Query("""
		SELECT bookmark.prompt.id
		FROM PromptBookmark bookmark
		WHERE bookmark.memberId = :memberId
		AND bookmark.prompt.visibility = com.chaean.promptdrive.prompt.internal.domain.PromptVisibility.PUBLIC
		ORDER BY bookmark.updatedAt DESC, bookmark.id DESC
		""")
	List<Long> findBookmarkedPublicPromptIds(@Param("memberId") Long memberId);

	@Query("""
		SELECT bookmark.prompt.id
		FROM PromptBookmark bookmark
		WHERE bookmark.memberId = :memberId
		AND bookmark.prompt.id IN :promptIds
		AND bookmark.prompt.visibility = com.chaean.promptdrive.prompt.internal.domain.PromptVisibility.PUBLIC
		""")
	List<Long> findBookmarkedPublicPromptIdsByMemberIdAndPromptIdIn(@Param("memberId") Long memberId,
		@Param("promptIds") List<Long> promptIds);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		INSERT INTO prompt_bookmark (prompt_id, member_id)
		SELECT prompt.id, :memberId
		FROM prompt
		WHERE prompt.id = :promptId
		AND prompt.visibility = 'PUBLIC'
		AND prompt.deleted_at IS NULL
		ON DUPLICATE KEY UPDATE member_id = member_id
		""", nativeQuery = true)
	int insertPublicPromptBookmarkIfAbsent(@Param("promptId") Long promptId, @Param("memberId") Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		UPDATE prompt_bookmark
		SET deleted_at = CURRENT_TIMESTAMP(6)
		WHERE prompt_id = :promptId
		AND member_id = :memberId
		AND deleted_at IS NULL
		""", nativeQuery = true)
	int softDeleteActiveByPromptIdAndMemberId(@Param("promptId") Long promptId, @Param("memberId") Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		UPDATE prompt_bookmark
		SET deleted_at = CURRENT_TIMESTAMP(6)
		WHERE prompt_id = :promptId
		AND deleted_at IS NULL
		""", nativeQuery = true)
	int softDeleteActiveByPromptId(@Param("promptId") Long promptId);
}
