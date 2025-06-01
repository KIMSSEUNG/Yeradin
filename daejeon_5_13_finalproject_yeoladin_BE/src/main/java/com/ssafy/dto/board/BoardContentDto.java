package com.ssafy.dto.board;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BoardContentDto {
  private int id;
  private int memberId ;
  private String boardId;
  private String author;
  private String content;
  private LocalDateTime createdTime;
}
