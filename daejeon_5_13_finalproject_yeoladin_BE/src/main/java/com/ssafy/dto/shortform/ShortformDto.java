package com.ssafy.dto.shortform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortformDto {
	private Integer pk;
	private String title;
	private String author;
	private String content;
	private int views;
	private int favoriteCount; // 좋아요 수
    private boolean favoritedByCurrentUser; // 현재 로그인한 사용자가 이 비디오를 좋아하는지 여부
	private LocalDateTime date;
	private String videofile;

	// ⭐ 추가: 조회 시 해당 숏폼과 연결된 콘텐츠 타입 이름 목록
    private List<String> contentTypes;

    // ⭐ 추가: 업로드 요청 시 선택된 콘텐츠 타입 ID (하나만 받음)
    // 이 필드는 주로 업로드 요청 DTO로 사용되지만, 현재 ShortformDto를 재활용하므로 여기에 추가
    private Integer selectedContentTypeId;
	
	public String getCreateTime() {
	    if (date == null) return "";
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	    return date.format(formatter);
	}
}
