package com.chaean.promptdrive.prompt.internal.web.community;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.community.MaintainCommunityPromptService;
import com.chaean.promptdrive.prompt.internal.dto.CreateCommunityPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.PromptDetailResponse;
import com.chaean.promptdrive.prompt.internal.dto.PromptSummaryResponse;
import com.chaean.promptdrive.prompt.internal.dto.UpdateCommunityPromptRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/my/prompts")
@RequiredArgsConstructor
public class CommunityPromptController {

	private final MaintainCommunityPromptService communityPromptService;

	@GetMapping
	public SliceResponse<PromptSummaryResponse> browse(Authentication authentication,
		@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer size) {
		return communityPromptService.browse(ownerMemberId(authentication), page, size);
	}

	@GetMapping("/{promptId}")
	public ApiResponse<PromptDetailResponse> get(Authentication authentication, @PathVariable Long promptId) {
		return ApiResponse.of(communityPromptService.get(ownerMemberId(authentication), promptId));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PromptDetailResponse>> create(Authentication authentication,
		@Valid @RequestBody CreateCommunityPromptRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.of(communityPromptService.create(ownerMemberId(authentication), request)));
	}

	@PutMapping("/{promptId}")
	public ApiResponse<PromptDetailResponse> update(Authentication authentication, @PathVariable Long promptId,
		@Valid @RequestBody UpdateCommunityPromptRequest request) {
		return ApiResponse.of(communityPromptService.update(ownerMemberId(authentication), promptId, request));
	}

	@DeleteMapping("/{promptId}")
	public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long promptId) {
		communityPromptService.delete(ownerMemberId(authentication), promptId);
		return ResponseEntity.noContent().build();
	}

	private Long ownerMemberId(Authentication authentication) {
		return Long.valueOf(authentication.getName());
	}
}
