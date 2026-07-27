package com.chaean.promptdrive.prompt.internal.web.catalog;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.common.web.response.SliceResponse;
import com.chaean.promptdrive.prompt.internal.application.catalog.MaintainCuratedPromptService;
import com.chaean.promptdrive.prompt.internal.domain.PromptVisibility;
import com.chaean.promptdrive.prompt.internal.dto.CreateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.CuratedPromptResponse;
import com.chaean.promptdrive.prompt.internal.dto.UpdateCuratedPromptRequest;
import com.chaean.promptdrive.prompt.internal.dto.UpdatePromptVisibilityRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptController {

	private final MaintainCuratedPromptService maintainCuratedPromptService;

	@GetMapping
	public SliceResponse<CuratedPromptResponse> browse(
		@RequestParam(required = false) PromptVisibility visibility,
		@RequestParam(defaultValue = "0") Integer page,
		@RequestParam(defaultValue = "20") Integer size
	) {
		return maintainCuratedPromptService.browse(visibility, page, size);
	}

	@GetMapping("/{promptId}")
	public ApiResponse<CuratedPromptResponse> get(@PathVariable Long promptId) {
		return ApiResponse.of(maintainCuratedPromptService.get(promptId));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CuratedPromptResponse>> create(
		@Valid @RequestBody CreateCuratedPromptRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.of(maintainCuratedPromptService.create(request)));
	}

	@PutMapping("/{promptId}")
	public ApiResponse<CuratedPromptResponse> update(
		@PathVariable Long promptId,
		@Valid @RequestBody UpdateCuratedPromptRequest request
	) {
		return ApiResponse.of(maintainCuratedPromptService.update(promptId, request));
	}

	@PatchMapping("/{promptId}/visibility")
	public ApiResponse<CuratedPromptResponse> changeVisibility(
		@PathVariable Long promptId,
		@Valid @RequestBody UpdatePromptVisibilityRequest request
	) {
		return ApiResponse.of(maintainCuratedPromptService.changeVisibility(promptId, request.getVisibility()));
	}

	@DeleteMapping("/{promptId}")
	public ResponseEntity<Void> delete(@PathVariable Long promptId) {
		maintainCuratedPromptService.delete(promptId);
		return ResponseEntity.noContent().build();
	}
}
