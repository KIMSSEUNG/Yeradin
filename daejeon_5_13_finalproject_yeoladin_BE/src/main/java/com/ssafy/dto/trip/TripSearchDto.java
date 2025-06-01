package com.ssafy.dto.trip;

import org.springframework.web.bind.annotation.RequestParam;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TripSearchDto {
	String sido ;
	String gugun ;
	String contentType;
}
