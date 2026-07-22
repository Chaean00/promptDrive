package com.chaean.promptdrive.common.web.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResponse<T> {

	private final List<T> content;
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;
	private final boolean first;
	private final boolean last;

	public static <T> PageResponse<T>
	from(Page<T> source) {
		return new PageResponse<>(
			List.copyOf(source.getContent()),
			source.getNumber(),
			source.getSize(),
			source.getTotalElements(),
			source.getTotalPages(),
			source.isFirst(),
			source.isLast()
		);
	}
}
