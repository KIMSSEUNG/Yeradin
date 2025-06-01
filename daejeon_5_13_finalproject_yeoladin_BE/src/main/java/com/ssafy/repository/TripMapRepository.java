package com.ssafy.repository;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.dto.trip.ContentData;
import com.ssafy.dto.trip.ContentDetailDto;
import com.ssafy.dto.trip.GugunInfo;

@Mapper
public interface TripMapRepository {
	
	
//	public TripContentDto getContentData() throws ClassNotFoundException, SQLException;
//	void gugunFind(Connection connection , TripContentDto dto) throws SQLException;
	
	List<GugunInfo> getSidoAndGugun();
	
	List<ContentData> searchAttractions(Map<String, Integer> param);
	int findGugunCode(@Param("gugunName") String gugun, @Param("sidoCode") int sidoCode);

	List<String> findByContents();

	ContentDetailDto getContentDetail(String title);
}
