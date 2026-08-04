package com.chaean.promptdrive.prompt.internal.web.copy;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.prompt.internal.application.copy.PromptCopyCommandService;
import com.chaean.promptdrive.prompt.internal.dto.PromptCopyResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prompts/{promptId}/copies")
@RequiredArgsConstructor
public class PromptCopyController {

	private final PromptCopyCommandService promptCopyCommandService;

	@PostMapping
	public ApiResponse<PromptCopyResponse> registerPromptCopy(@PathVariable Long promptId) {
		return ApiResponse.of(promptCopyCommandService.registerPromptCopy(promptId));
	}
}
