package com.chaean.promptdrive.common.web.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.data.domain.Slice;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SliceResponse<T> {

	private final List<T> content;
	private final int page;
	private final int size;
	private final boolean first;
	private final boolean last;
	private final boolean hasNext;

	public static <T> SliceResponse<T> from(Slice<T> source) {
		return new SliceResponse<>(
			List.copyOf(source.getContent()),
			source.getNumber(),
			source.getSize(),
			source.isFirst(),
			source.isLast(),
			source.hasNext()
		);
	}
}
