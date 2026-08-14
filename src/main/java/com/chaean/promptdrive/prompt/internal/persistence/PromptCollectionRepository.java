package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptCollectionRepository extends JpaRepository<PromptCollection, Long> {

	Optional<PromptCollection> findBySlug(String slug);

	List<PromptCollection> findAllByOrderByUpdatedAtDescIdDesc();
}
