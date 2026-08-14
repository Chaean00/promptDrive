package com.chaean.promptdrive.prompt.internal.web.bookmark;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.bookmark.PromptBookmarkCommandService;
import com.chaean.promptdrive.prompt.internal.application.bookmark.PromptBookmarkQueryService;
import com.chaean.promptdrive.prompt.internal.dto.PromptBookmarkResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptBookmarkStatusRequest;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;

import org.springframework.security.core.Authentication;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PromptBookmarkController {

	private final PromptBookmarkCommandService promptBookmarkCommandService;
	private final PromptBookmarkQueryService promptBookmarkQueryService;

	@GetMapping("/my/bookmarked-prompts")
	public SliceResponse<PromptSummaryResponse> listBookmarkedPrompts(Authentication authentication,
		@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size) {
		return promptBookmarkQueryService.getBookmarkedPromptPage(resolveAuthenticatedMemberId(authentication), page, size);
	}

	@GetMapping("/my/bookmarked-prompt-ids")
	@Deprecated(forRemoval = false)
	public ApiResponse<List<Long>> listBookmarkedPromptIds(Authentication authentication) {
		return ApiResponse.of(promptBookmarkQueryService.getBookmarkedPromptIds(resolveAuthenticatedMemberId(authentication)));
	}

	@PostMapping("/my/bookmark-statuses")
	public ApiResponse<List<Long>> listBookmarkedPromptStatuses(Authentication authentication,
		@Valid @RequestBody PromptBookmarkStatusRequest request) {
		return ApiResponse.of(promptBookmarkQueryService.getBookmarkedPromptIds(resolveAuthenticatedMemberId(authentication), request.getPromptIds()));
	}

	@PostMapping("/prompts/{promptId}/bookmarks")
	public ApiResponse<PromptBookmarkResponse> registerPromptBookmark(Authentication authentication, @PathVariable Long promptId) {
		return ApiResponse.of(promptBookmarkCommandService.registerPromptBookmark(resolveAuthenticatedMemberId(authentication), promptId));
	}

	@DeleteMapping("/prompts/{promptId}/bookmarks")
	public ApiResponse<PromptBookmarkResponse> removePromptBookmark(Authentication authentication, @PathVariable Long promptId) {
		return ApiResponse.of(promptBookmarkCommandService.removePromptBookmark(resolveAuthenticatedMemberId(authentication), promptId));
	}

	private Long resolveAuthenticatedMemberId(Authentication authentication) {
		return Long.valueOf(authentication.getName());
	}
}
