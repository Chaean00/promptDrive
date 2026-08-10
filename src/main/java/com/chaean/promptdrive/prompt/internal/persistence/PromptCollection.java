package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.Objects;

import com.chaean.promptdrive.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "prompt_collection")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptCollection extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String slug;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Version
	@Column(nullable = false)
	private long version;

	private PromptCollection(String slug, String title, String description) {
		this.slug = Objects.requireNonNull(slug);
		this.title = Objects.requireNonNull(title);
		this.description = Objects.requireNonNull(description);
	}

	public static PromptCollection create(String slug, String title, String description) {
		return new PromptCollection(slug, title, description);
	}

	public void update(String slug, String title, String description) {
		this.slug = Objects.requireNonNull(slug);
		this.title = Objects.requireNonNull(title);
		this.description = Objects.requireNonNull(description);
	}
}
