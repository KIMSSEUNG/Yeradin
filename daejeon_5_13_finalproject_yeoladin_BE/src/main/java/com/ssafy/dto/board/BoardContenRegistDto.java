package com.ssafy.dto.board;

import lombok.Data;

@Data
public class BoardContenRegistDto {
  private int memberId ;
  private String boardId;
  private String content;
}
