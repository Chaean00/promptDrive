package com.chaean.promptdrive.prompt.internal.persistence;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.DynamicUpdate;

import com.chaean.promptdrive.common.persistence.BaseEntity;
import com.chaean.promptdrive.common.web.error.CommonErrorCode;
import com.chaean.promptdrive.common.web.error.exception.BusinessException;
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
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "prompt", indexes = {
		@Index(name = "idx_prompt_owner_member_id", columnList = "owner_member_id")
})
@SQLRestriction("deleted_at IS NULL")
@DynamicUpdate
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

	@Column(name = "copy_count", nullable = false, updatable = false)
	private long copyCount;

	@Version
	@Column(nullable = false)
	private long version;

	private Prompt(
			String title,
			String content,
			PromptProvenance provenance,
			PromptVisibility visibility,
			Long ownerMemberId,
			String sourceName,
			String sourceUrl
	) {
		this.title = Objects.requireNonNull(title);
		this.content = Objects.requireNonNull(content);
		this.provenance = Objects.requireNonNull(provenance);
		this.visibility = Objects.requireNonNull(visibility);
		if ((provenance == PromptProvenance.CURATED && ownerMemberId != null) || (provenance == PromptProvenance.COMMUNITY && ownerMemberId == null)) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		this.ownerMemberId = ownerMemberId;
		this.sourceName = sourceName;
		this.sourceUrl = sourceUrl;
	}

	public static Prompt createCuratedPrompt(String title, String content, PromptVisibility visibility,
			String sourceName, String sourceUrl) {
		return new Prompt(title, content, PromptProvenance.CURATED, visibility, null, sourceName, sourceUrl);
	}

	public static Prompt createCommunityPrompt(Long ownerMemberId, String title, String content) {
		return new Prompt(title, content, PromptProvenance.COMMUNITY, PromptVisibility.PUBLIC, ownerMemberId, null, null);
	}

	public void updateCuratedPrompt(String title, String content, String sourceName, String sourceUrl) {
		assertCuratedPrompt();
		this.title = Objects.requireNonNull(title);
		this.content = Objects.requireNonNull(content);
		this.sourceName = sourceName;
		this.sourceUrl = sourceUrl;
	}

	public void changeCuratedPromptVisibility(PromptVisibility visibility) {
		assertCuratedPrompt();
		this.visibility = Objects.requireNonNull(visibility);
	}

	public void softDeleteCuratedPrompt() {
		assertCuratedPrompt();
		softDelete();
	}

	public void updateCommunityPrompt(Long ownerMemberId, String title, String content) {
		assertCommunityPromptOwner(ownerMemberId);
		this.title = Objects.requireNonNull(title);
		this.content = Objects.requireNonNull(content);
	}

	public void softDeleteCommunityPrompt(Long ownerMemberId) {
		assertCommunityPromptOwner(ownerMemberId);
		softDelete();
	}

	private void assertCuratedPrompt() {
		if (provenance != PromptProvenance.CURATED || ownerMemberId != null) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private void assertCommunityPromptOwner(Long ownerMemberId) {
		if (provenance != PromptProvenance.COMMUNITY || ownerMemberId == null
			|| !Objects.equals(this.ownerMemberId, ownerMemberId)) {
			throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
		}
	}
}
