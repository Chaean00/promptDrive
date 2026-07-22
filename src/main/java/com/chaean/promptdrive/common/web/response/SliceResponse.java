package com.chaean.promptdrive.common.web.response;

import java.util.List;

import lombok.Getter;

import org.springframework.data.domain.Slice;

@Getter
public class SliceResponse<T> {

	private final List<T> content;
	private final int page;
	private final int size;
	private final boolean first;
	private final boolean last;
	private final boolean hasNext;

	private SliceResponse(Slice<T> source) {
		this.content = List.copyOf(source.getContent());
		this.page = source.getNumber();
		this.size = source.getSize();
		this.first = source.isFirst();
		this.last = source.isLast();
		this.hasNext = source.hasNext();
	}

	public static <T> SliceResponse<T> from(Slice<T> source) {
		return new SliceResponse<>(source);
	}
}
