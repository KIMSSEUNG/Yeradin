package com.ssafy.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ImageDto {
	private String originalName;
	private String storeName;
	private String boardId;
}
