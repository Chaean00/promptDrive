package com.chaean.promptdrive.prompt.internal.dto;

import java.util.List;

import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommunityPromptRequest {

	@NotBlank
	@Size(max = 200)
	private String title;

	@NotBlank
	private String content;

	@NotNull
	private List<PromptCategoryType> categories;
}
