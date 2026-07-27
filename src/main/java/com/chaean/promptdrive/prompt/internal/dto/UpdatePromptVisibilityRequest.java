package com.chaean.promptdrive.prompt.internal.dto;

import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePromptVisibilityRequest {

	@NotNull
	private PromptVisibility visibility;
}
