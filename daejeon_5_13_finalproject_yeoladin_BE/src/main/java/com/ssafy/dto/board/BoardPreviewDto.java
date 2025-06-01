package com.ssafy.dto.board;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardPreviewDto {
	private String id;                // 게시글 고유 ID
    private String title;           // 제목
    private String contentPriview;
    private int memberId;          // 작성자 이름 또는 ID
    private String author;
    private String imgForderPath;
    private String thumbnailUrl;
    private LocalDateTime createdTime; // 작성일자
    
	public String getCreateTime() {
	    if (createdTime == null) return "";
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	    return createdTime.format(formatter);
	}
}
