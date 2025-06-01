package com.ssafy.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.dto.shortform.ShortformDto;
import com.ssafy.service.ShortformService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainPageRestController {
	private final ShortformService service;
	
	@GetMapping("/popular-shortforms")
    public ResponseEntity<List<ShortformDto>> getPopularShortforms() {
        try {
            // 조회수 상위 5개 숏폼 가져오기
            List<ShortformDto> popularShortforms = service.getPopularShortforms(5);
            if (popularShortforms != null && !popularShortforms.isEmpty()) {
                return ResponseEntity.ok(popularShortforms);
            } else {
                return ResponseEntity.noContent().build(); // 데이터가 없을 경우 204 No Content
            }
        } catch (Exception e) {
            log.error("Error fetching popular shortforms: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
