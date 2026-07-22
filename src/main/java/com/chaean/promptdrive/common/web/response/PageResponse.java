package com.chaean.promptdrive.common.web.response;

import java.util.List;

import lombok.Getter;

import org.springframework.data.domain.Page;

@Getter
public class PageResponse<T> {

	private final List<T> content;
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;
	private final boolean first;
	private final boolean last;

	private PageResponse(Page<T> source) {
		this.content = List.copyOf(source.getContent());
		this.page = source.getNumber();
		this.size = source.getSize();
		this.totalElements = source.getTotalElements();
		this.totalPages = source.getTotalPages();
		this.first = source.isFirst();
		this.last = source.isLast();
	}

	public static <T> PageResponse<T> from(Page<T> source) {
		return new PageResponse<>(source);
	}
}
