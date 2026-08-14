package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptCollectionItemRepository extends JpaRepository<PromptCollectionItem, Long> {

	List<PromptCollectionItem> findAllByCollectionId(Long collectionId);

	@Query("""
		SELECT i FROM PromptCollectionItem i
		WHERE i.collection.id = :collectionId
		AND i.prompt.visibility = com.chaean.promptdrive.prompt.internal.domain.PromptVisibility.PUBLIC
		ORDER BY i.displayOrder ASC, i.id ASC
		""")
	List<PromptCollectionItem> findPublicItemsByCollectionId(@Param("collectionId") Long collectionId);

	@Query("""
		SELECT i.collection.id AS collectionId, COUNT(i) AS promptCount
		FROM PromptCollectionItem i
		WHERE i.prompt.visibility = com.chaean.promptdrive.prompt.internal.domain.PromptVisibility.PUBLIC
		GROUP BY i.collection.id
		""")
	List<PromptCollectionPromptCount> countPublicPromptsByCollection();
}
