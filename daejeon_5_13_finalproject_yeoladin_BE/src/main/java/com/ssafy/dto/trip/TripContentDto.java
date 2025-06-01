package com.ssafy.dto.trip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TripContentDto {
	
	private List<String> sido;
	private HashMap<String,List<String>> gun;
	private List<String> contents;
	
}
