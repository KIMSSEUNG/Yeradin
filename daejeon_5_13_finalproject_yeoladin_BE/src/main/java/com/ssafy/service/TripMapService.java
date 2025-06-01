package com.ssafy.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ssafy.dto.trip.ContentData;
import com.ssafy.dto.trip.ContentDetailDto;
import com.ssafy.dto.trip.ContentSearchDto;
import com.ssafy.dto.trip.GugunInfo;
import com.ssafy.dto.trip.TripContentDto;
import com.ssafy.repository.TripMapRepository;

import lombok.RequiredArgsConstructor;

@Service
public class TripMapService {

	private final TripMapRepository tripMapRepo;
	
	private HashMap<String, Integer> sidoMap = new HashMap<>();
	private HashMap<Integer, HashMap<String ,Integer>> gugun = new HashMap<>();
	private HashMap<String, Integer> culturalFacilityMap = new HashMap<>();
	
	@Autowired
	public TripMapService(TripMapRepository tripMapRepo) {
		this.tripMapRepo=tripMapRepo;
		init();
	}
	
	private void init() {
		//시도
		sidoMap.put("서울", 1);
        sidoMap.put("인천", 2);
        sidoMap.put("대전", 3);
        sidoMap.put("대구", 4);
        sidoMap.put("광주", 5);
        sidoMap.put("부산", 6);
        sidoMap.put("울산", 7);
        sidoMap.put("세종특별자치시", 8);
        sidoMap.put("경기도", 31);
        sidoMap.put("강원특별자치도", 32);
        sidoMap.put("충청북도", 33);
        sidoMap.put("충청남도", 34);
        sidoMap.put("경상북도", 35);
        sidoMap.put("경상남도", 36);
        sidoMap.put("전북특별자치도", 37);
        sidoMap.put("전라남도", 38);
        sidoMap.put("제주도", 39);
        
        //구군
        
        //컨텐트
        culturalFacilityMap.put("관광지", 12); // 마지막 값으로 덮어씀 (중복 key 처리됨)
        culturalFacilityMap.put("문화시설", 14);
        culturalFacilityMap.put("축제공연행사", 15);
        culturalFacilityMap.put("여행코스", 25);
        culturalFacilityMap.put("레포츠", 28);
        culturalFacilityMap.put("숙박", 32);
        culturalFacilityMap.put("쇼핑", 38);
        culturalFacilityMap.put("음식점", 39);
	}
	

	public TripContentDto findContent() {
	    List<GugunInfo> rawList = tripMapRepo.getSidoAndGugun();

	    List<String> sidoList = new ArrayList<>();
	    Map<String, List<String>> gunMap = new HashMap<>();

	    for (GugunInfo info : rawList) {
	        String sido = info.getSido();
	        String gugun = info.getGugun();

	        // sido 중복 방지
	        if (!sidoList.contains(sido)) {
	            sidoList.add(sido);
	        }

	        // gugun 맵 구성
	        gunMap.computeIfAbsent(sido, k -> new ArrayList<>()).add(gugun);
	    }
	    List<String> contents = tripMapRepo.findByContents();
	    contents.remove(contents.size()-1);
	    contents.add(contents.size()-1,"음식점");
	    
	    return new TripContentDto(sidoList, new HashMap<>(gunMap) , contents);
	}

	
	
	public ContentSearchDto contentSearch(ContentSearchDto dto) {
	    System.out.println(dto.toString());
		int sidoCode = sidoMap.get(dto.getSido());
	    int contentTypeId = culturalFacilityMap.get(dto.getContent());
	    int gugunCode = tripMapRepo.findGugunCode(dto.getGugun(), sidoCode);

	    Map<String, Integer> param = new HashMap<>();
	    param.put("sidoCode", sidoCode);
	    param.put("contentTypeId", contentTypeId);
	    param.put("gugunCode", gugunCode);

	    List<ContentData> results = tripMapRepo.searchAttractions(param);
	    dto.setContents(results);
	    return dto;
	}

	public ContentDetailDto contentDetail(String title) {
		return tripMapRepo.getContentDetail(title);
	}


}
