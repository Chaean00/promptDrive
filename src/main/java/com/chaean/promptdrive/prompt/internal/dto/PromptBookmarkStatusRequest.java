package com.chaean.promptdrive.prompt.internal.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PromptBookmarkStatusRequest {

	@NotNull
	@Size(max = 100)
	private List<@NotNull Long> promptIds;
}
