package com.ssafy.dto.trip;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ContentSearchDto {
	
	private String sido;
	private String gugun;
	private String content;
	private List<ContentData> contents = new ArrayList<>();
	
	
}
