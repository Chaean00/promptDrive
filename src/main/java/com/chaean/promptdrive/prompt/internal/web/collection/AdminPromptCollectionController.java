package com.chaean.promptdrive.prompt.internal.web.collection;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.prompt.internal.application.collection.PromptCollectionCommandService;
import com.chaean.promptdrive.prompt.internal.dto.CreatePromptCollectionRequest;
import com.chaean.promptdrive.prompt.internal.dto.PromptCollectionResponse;
import com.chaean.promptdrive.prompt.internal.dto.UpdatePromptCollectionRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/prompt-collections")
@RequiredArgsConstructor
public class AdminPromptCollectionController {

	private final PromptCollectionCommandService commandService;

	@PostMapping
	public ResponseEntity<ApiResponse<PromptCollectionResponse>> create(
		@Valid @RequestBody CreatePromptCollectionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(commandService.create(request)));
	}

	@PutMapping("/{collectionId}")
	public ApiResponse<PromptCollectionResponse> update(@PathVariable Long collectionId,
		@Valid @RequestBody UpdatePromptCollectionRequest request) {
		return ApiResponse.of(commandService.update(collectionId, request));
	}

	@DeleteMapping("/{collectionId}")
	public ResponseEntity<Void> delete(@PathVariable Long collectionId) {
		commandService.delete(collectionId);
		return ResponseEntity.noContent().build();
	}
}
