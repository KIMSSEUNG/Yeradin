package com.ssafy.ai;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dto.trip.TravelPlan;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import org.springframework.ai.chat.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGptUtil {
	private final ChatClient.Builder chatClientBuilder;

	public List<TravelPlan> recommendRoute(String region) {
	    ChatClient chatClient = chatClientBuilder.build();

	    StringBuilder sb = new StringBuilder();
	    setChatgptMessage(sb, region);

	    // 1. GPT 호출 및 JSON 응답 수신
	    String json = chatClient.prompt()
	            .user(sb.toString())
	            .call()
	            .content();
	    json = json.replaceAll("(?s)```json|```", "").trim();
	    // 2. JSON → 객체로 파싱
	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        return mapper.readValue(json, new TypeReference<List<TravelPlan>>() {});
	    } catch (Exception e) {
	        throw new RuntimeException("AI 응답 파싱 실패", e);
	    }
	}
	
	
	
	private void setChatgptMessage(StringBuilder sb , String region) {
	    sb.append("지역 \"").append(region).append("\"을 기반으로 3일간 여행 경로를 JSON 형식으로 생성해줘.\n");
	    sb.append("조건:\n");
	    sb.append("각 일자마다 3개 장소 방문.\n");
	    sb.append("lat: 위도 (소수점), lng: 경도 (소수점), title: 장소 이름, durationFromPrev: 이전 장소로부터의 이동 시간 문자열.\n");
	    sb.append("JSON 배열 형식으로, 다음과 같이 예시를 따라줘:\n\n");
	    
	    sb.append("[\n");
	    sb.append("  {\n");
	    sb.append("    \"day\": 1,\n");
	    sb.append("    \"route\": [\n");
	    sb.append("      {\n");
	    sb.append("        \"title\": \"해운대 해수욕장\",\n");
	    sb.append("        \"lat\": 35.1587,\n");
	    sb.append("        \"lng\": 129.1604,\n");
	    sb.append("        \"durationFromPrev\": \"출발\"\n");
	    sb.append("      },\n");
	    sb.append("      {\n");
	    sb.append("        \"title\": \"동백섬\",\n");
	    sb.append("        \"lat\": 35.1551,\n");
	    sb.append("        \"lng\": 129.1485,\n");
	    sb.append("        \"durationFromPrev\": \"도보 15분\"\n");
	    sb.append("      }\n");
	    sb.append("    ]\n");
	    sb.append("  }\n");
	    sb.append("]");
	}

}
