package com.ssafy.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.ai.ChatGptUtil;
import com.ssafy.dto.trip.ContentDetailDto;
import com.ssafy.dto.trip.ContentSearchDto;
import com.ssafy.dto.trip.TravelPlan;
import com.ssafy.dto.trip.TripContentDto;
import com.ssafy.dto.trip.TripSearchDto;
import com.ssafy.service.TripMapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/tripMap")
@RequiredArgsConstructor
public class TripMapRestController {
	
	private final TripMapService service;
	private final ChatGptUtil gptUtil;
	
	@GetMapping("/content")
	public TripContentDto registTripContent() throws Exception {
		TripContentDto dto =  service.findContent();
		return dto;
	}
	
	@GetMapping("/filter")
	public ResponseEntity<?> searchContentXY(@ModelAttribute TripSearchDto dto) throws Exception {
		ContentSearchDto SearchDto = new ContentSearchDto(dto.getSido() , dto.getGugun() , dto.getContentType() , new ArrayList<>());
		SearchDto =  service.contentSearch(SearchDto);
		return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(SearchDto);
	}
	
	@GetMapping("/detail")
	public ContentDetailDto contentDetail(@RequestParam String title) throws Exception {
		return service.contentDetail(title);
	}
	
	@GetMapping("/ai")
	public List<TravelPlan> aiRecommendRoute(@RequestParam String region) throws Exception {
		List<TravelPlan> plan = gptUtil.recommendRoute(region);
		System.out.println(plan.toString());
		return plan;
	}
	
}
