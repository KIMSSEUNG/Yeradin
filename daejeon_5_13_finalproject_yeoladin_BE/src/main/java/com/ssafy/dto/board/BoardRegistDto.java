package com.ssafy.dto.board;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BoardRegistDto {
	private String id;
	private String title;
	private String content;
	private int memberId;
	private String author;
	private String imgForderPath;
	private String thumbnailUrl;
    private String contentPriview;
	MultipartFile[] images;
	
	public MultipartFile[] getImages() {
		if(images==null) {
			return new MultipartFile[0];
		}
		else {
			return images;
		}
	}
	
}
