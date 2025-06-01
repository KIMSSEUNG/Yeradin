package com.ssafy.dto.board;


import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class BoardUpdateDto {
	private String id;
	private String title;
	private String content;
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

