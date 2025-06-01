package com.ssafy.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.dto.board.BoardContenRegistDto;
import com.ssafy.dto.board.BoardContentDto;
import com.ssafy.dto.board.BoardDetailDto;
import com.ssafy.dto.board.BoardPreviewDto;
import com.ssafy.dto.board.BoardRegistDto;
import com.ssafy.dto.board.BoardSearchDto;
import com.ssafy.dto.board.ImageDto;

@Mapper
public interface BoardRepository {
	public List<BoardPreviewDto> findAll() throws SQLException, ClassNotFoundException;
	public List<BoardPreviewDto> search(Map<String, Object> param)throws SQLException, ClassNotFoundException;
	public void save(BoardRegistDto dto);
	public void imageSave(List<ImageDto> imgList);
	public BoardDetailDto findDetail(String id);
	public String findByForderPath(String id);
	public void delete(String id);
	public List<BoardPreviewDto> findByBoards(Map<String, Object> param);
	public int countBoards();
	public int countFilter(BoardSearchDto dto);
	public void contentSave(BoardContenRegistDto dto);
	public List<BoardContentDto> findByBoardComments(String boardId);
	public void deleteComment(int id);
}
