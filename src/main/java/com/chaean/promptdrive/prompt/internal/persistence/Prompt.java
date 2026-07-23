package com.chaean.promptdrive.prompt.internal.persistence;

import org.hibernate.annotations.SQLRestriction;

import com.chaean.promptdrive.common.persistence.BaseEntity;
import com.chaean.promptdrive.prompt.internal.domain.PromptProvenance;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "prompt", indexes = @Index(name = "idx_prompt_owner_member_id", columnList = "owner_member_id"))
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prompt extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PromptProvenance provenance;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PromptVisibility visibility;

	@Column(name = "owner_member_id")
	private Long ownerMemberId;

	@Column(name = "source_name", length = 255)
	private String sourceName;

	@Column(name = "source_url", length = 2048)
	private String sourceUrl;

	public Prompt(
			String title,
			String content,
			PromptProvenance provenance,
			PromptVisibility visibility,
			Long ownerMemberId,
			String sourceName,
			String sourceUrl
	) {
		this.title = title;
		this.content = content;
		this.provenance = provenance;
		this.visibility = visibility;
		this.ownerMemberId = ownerMemberId;
		this.sourceName = sourceName;
		this.sourceUrl = sourceUrl;
	}
}
