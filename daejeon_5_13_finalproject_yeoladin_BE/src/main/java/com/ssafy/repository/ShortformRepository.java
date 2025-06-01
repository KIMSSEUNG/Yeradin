package com.ssafy.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.dto.contenttype.ContentTypeDto;
import com.ssafy.dto.shortform.ShortformContentTypeMappingDto;
import com.ssafy.dto.shortform.ShortformDto;

@Mapper
public interface ShortformRepository {
    int insertVideo(ShortformDto formDto) throws Exception;
    List<ShortformDto> selectAllVideo() throws Exception;
    ShortformDto selectVideo(int pk) throws Exception;         // 변경: String id → int pk
    int updateVideo(ShortformDto formDto) throws Exception;     // dto 안에 pk 포함
    int deleteVideo(int pk) throws Exception;                   // 변경: String id → int pk
    int incrementViewCount(int pk) throws Exception;

 // ⭐ 추가: 조회수 순으로 상위 N개 숏폼 조회
    List<ShortformDto> findPopularShortforms(@Param("limit") int limit) throws Exception;
    
    // ⭐ 추가: 콘텐츠 타입 목록 조회
    List<ContentTypeDto> findAllContentTypes() throws Exception;

    // ⭐ 추가: shortform_to_contenttype 테이블에 매핑 정보 삽입
    void insertShortformContentType(@Param("shortformPk") int shortformPk, @Param("contentTypeId") int contentTypeId) throws Exception;

    // ⭐ 추가: 특정 숏폼과 연결된 콘텐츠 타입 이름 목록 조회 (단건 조회 시 사용)
    List<String> findContentTypeNamesByShortformPk(int shortformPk) throws Exception;

    // ⭐ 추가: 모든 숏폼-콘텐츠타입 매핑 정보 조회 (전체 목록 조회 시 사용)
    List<ShortformContentTypeMappingDto> findAllShortformContentTypeMappings() throws Exception;
    
    // ⭐ 추가: 특정 콘텐츠 타입 이름으로 숏폼 목록 조회 (관련 쇼츠 조회 시 사용)
    List<ShortformDto> findVideosByContentTypeName(@Param("contentTypeName") String contentTypeName) throws Exception;

    // ⭐ 추가: 특정 숏폼 PK 목록에 대한 콘텐츠 타입 매핑 정보 조회
    List<ShortformContentTypeMappingDto> findShortformContentTypeMappingsByPks(@Param("pks") List<Integer> pks) throws Exception;
    
    // ⭐ 추가: 특정 멤버가 특정 숏폼 PK 목록 중 좋아요 누른 숏폼 PK 목록 조회
    List<Integer> findFavoritedShortformPksByMember(@Param("memberId") Long memberId, @Param("shortformPks") List<Integer> shortformPks) throws Exception;

 // ⭐ 추가: 특정 숏폼의 모든 콘텐츠 타입 매핑 삭제 (업데이트 시 사용)
    int deleteShortformContentTypeMappings(@Param("shortformPk") int shortformPk) throws Exception;
    
 // --- 좋아요 관련 메서드 추가 ---
    int isFavorite(@Param("memberId") Long memberId, @Param("shortformPk") int shortformPk) throws Exception;
    void addFavorite(@Param("memberId") Long memberId, @Param("shortformPk") int shortformPk) throws Exception;
    void removeFavorite(@Param("memberId") Long memberId, @Param("shortformPk") int shortformPk) throws Exception;
    void incrementFavoriteCount(int pk) throws Exception;
    void decrementFavoriteCount(int pk) throws Exception;
    Integer getFavoriteCount(int pk) throws Exception; // 좋아요 수만 가져오는 메서드 (null 가능성 고려)
}
