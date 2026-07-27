package com.chaean.promptdrive.prompt.internal.web.category;

import java.util.Arrays;
import java.util.List;

import com.chaean.promptdrive.common.web.response.ApiResponse;
import com.chaean.promptdrive.prompt.internal.domain.PromptCategoryType;
import com.chaean.promptdrive.prompt.internal.dto.EnumDisplayResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompt-categories")
public class CategoryController {

	@GetMapping
	public ApiResponse<List<EnumDisplayResponse>> list() {
		return ApiResponse.of(Arrays.stream(PromptCategoryType.values()).map(EnumDisplayResponse::from).toList());
	}
}
