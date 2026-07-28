package com.chaean.promptdrive.prompt.internal.web.like;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.prompt.internal.application.like.PromptLikeCommandService;
import com.chaean.promptdrive.prompt.internal.dto.PromptLikeResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prompts/{promptId}/likes")
@RequiredArgsConstructor
public class PromptLikeController {

	private final PromptLikeCommandService promptLikeCommandService;

	@PostMapping
	public ApiResponse<PromptLikeResponse> registerPromptLike(Authentication authentication, @PathVariable Long promptId) {
		return ApiResponse.of(promptLikeCommandService.registerPromptLike(resolveAuthenticatedMemberId(authentication), promptId));
	}

	@DeleteMapping
	public ApiResponse<PromptLikeResponse> removePromptLike(Authentication authentication, @PathVariable Long promptId) {
		return ApiResponse.of(promptLikeCommandService.removePromptLike(resolveAuthenticatedMemberId(authentication), promptId));
	}

	private Long resolveAuthenticatedMemberId(Authentication authentication) {
		return Long.valueOf(authentication.getName());
	}
}
