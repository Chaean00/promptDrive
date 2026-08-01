package com.chaean.promptdrive.prompt.internal.persistence;

import org.hibernate.annotations.SQLRestriction;

import com.chaean.promptdrive.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "prompt_like", indexes = @Index(name = "idx_prompt_like_member_id_prompt_id", columnList = "member_id, prompt_id"))
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptLike extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prompt_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Prompt prompt;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	public PromptLike(Prompt prompt, Long memberId) {
		this.prompt = prompt;
		this.memberId = memberId;
	}
}
