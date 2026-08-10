package com.chaean.promptdrive.prompt.internal.persistence;

import java.util.Objects;

import com.chaean.promptdrive.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "prompt_collection_item")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptCollectionItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "collection_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private PromptCollection collection;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prompt_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Prompt prompt;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	private PromptCollectionItem(PromptCollection collection, Prompt prompt, int displayOrder) {
		this.collection = Objects.requireNonNull(collection);
		this.prompt = Objects.requireNonNull(prompt);
		this.displayOrder = displayOrder;
	}

	public static PromptCollectionItem create(PromptCollection collection, Prompt prompt, int displayOrder) {
		return new PromptCollectionItem(collection, prompt, displayOrder);
	}
}
