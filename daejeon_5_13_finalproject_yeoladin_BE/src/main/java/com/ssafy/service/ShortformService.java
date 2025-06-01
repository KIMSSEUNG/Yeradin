package com.ssafy.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dto.contenttype.ContentTypeDto;
import com.ssafy.dto.shortform.ShortformContentTypeMappingDto;
import com.ssafy.dto.shortform.ShortformDto;
import com.ssafy.repository.ShortformRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortformService {
	private final ShortformRepository formRepo; 
	
	public int insertVideo(ShortformDto form, Long memberId) throws Exception{ // memberId는 필요하다면 서비스에서 사용
		// 1. shortform 테이블에 삽입
		int shortformInsertCount = formRepo.insertVideo(form); // 이 시점에 form 객체의 pk 필드에 생성된 PK가 담김

        // 삽입 실패 시 예외 처리
        if (shortformInsertCount == 0 || form.getPk() == null) {
            throw new RuntimeException("Failed to insert shortform video.");
        }

        // 2. shortform_to_contenttype 테이블에 삽입 (선택된 타입이 있다면)
        // 사용자가 한 개만 선택해서 보내므로, 해당 한 개의 ID를 사용
        Integer selectedContentTypeId = form.getSelectedContentTypeId(); // DTO에서 선택된 ID 가져옴

        if (selectedContentTypeId != null) {
            log.debug("Inserting mapping for shortform PK {} and contentTypeId {}", form.getPk(), selectedContentTypeId);
            // form.getPk()로 얻은 PK와 selectedContentTypeId를 사용하여 삽입
            formRepo.insertShortformContentType(form.getPk(), selectedContentTypeId);
        } else {
             // required = false 였다면 여기 로직 필요. required = true 라면 컨트롤러에서 걸러짐.
             log.warn("No contentTypeId provided for shortform upload PK: {}", form.getPk());
             // 선택이 필수라면 여기서 예외를 던지거나 롤백 고려
             throw new IllegalArgumentException("콘텐츠 타입이 선택되지 않았습니다.");
        }

		return shortformInsertCount; // 숏폼 삽입 성공 개수 반환
	}
	
	public ShortformDto selectVideo(int pk, Long memberId) throws Exception{
		ShortformDto video = formRepo.selectVideo(pk); // 숏폼 기본 정보 조회

        if (video != null) {
            // 해당 숏폼과 연결된 콘텐츠 타입 이름 목록 조회
            List<String> contentTypes = formRepo.findContentTypeNamesByShortformPk(pk);
            video.setContentTypes(contentTypes); // DTO에 설정

            // 로그인한 사용자라면 좋아요 여부 확인
            if (memberId != null) {
                video.setFavoritedByCurrentUser(formRepo.isFavorite(memberId, pk) > 0);
            } else {
                video.setFavoritedByCurrentUser(false); // memberId가 없으면 좋아요 안 한 것으로 처리
            }
        }
        return video;
	}
	
	public List<ShortformDto> getPopularShortforms(int limit) throws Exception {
        // 이 단계에서는 각 숏폼의 contentTypes나 favoritedByCurrentUser를 로드하지 않습니다.
        // 메인 페이지 목록에서는 간단한 정보만 필요하다고 가정합니다.
        // 만약 해당 정보가 필요하다면, selectAllVideo와 유사한 방식으로 처리해야 합니다.
        return formRepo.findPopularShortforms(limit);
    }
	
	public List<ContentTypeDto> getAllContentTypes() throws Exception {
        return formRepo.findAllContentTypes();
    }
	
	// READ - 전체 조회 (콘텐츠 타입 정보 포함)
	// N+1 문제를 피하기 위해, 비디오 목록 전체를 가져온 후, 모든 매핑 정보를 가져와서 매핑하는 방식
	public List<ShortformDto> selectAllVideo(Long memberId) throws Exception{
		List<ShortformDto> videos = formRepo.selectAllVideo(); // 모든 숏폼 기본 정보 조회

        if (videos.isEmpty()) {
            return videos; // 비디오가 없으면 바로 반환
        }

        // 모든 숏폼-콘텐츠타입 매핑 정보 조회
        List<ShortformContentTypeMappingDto> allMappings = formRepo.findAllShortformContentTypeMappings();

        // 매핑 정보를 Shortform PK 별로 그룹화 (Map<Integer, List<String>>)
        Map<Integer, List<String>> contentTypeMap = allMappings.stream()
            .collect(Collectors.groupingBy(
                ShortformContentTypeMappingDto::getShortformPk, // 키: shortformPk
                Collectors.mapping( // 값: 콘텐츠 타입 이름 리스트
                    ShortformContentTypeMappingDto::getContentTypeName,
                    Collectors.toList()
                )
            ));

        // 각 비디오 DTO에 해당하는 콘텐츠 타입 목록 설정
        for (ShortformDto video : videos) {
            List<String> types = contentTypeMap.getOrDefault(video.getPk(), new ArrayList<>());
            video.setContentTypes(types); // DTO에 설정

            // 로그인한 사용자라면 각 비디오에 대한 좋아요 여부 확인 (이 부분은 여전히 N+1 가능성 있음, Bulk Select 고려)
            // 하지만 비디오 개수가 많지 않다면 이 방법도 허용 가능
             if (memberId != null) {
                 video.setFavoritedByCurrentUser(formRepo.isFavorite(memberId, video.getPk()) > 0);
             } else {
                 video.setFavoritedByCurrentUser(false); // memberId가 없으면 좋아요 안 한 것으로 처리
             }
        }


        return videos;
	}
	
	public List<ShortformDto> selectVideosByContentTypeName(String contentTypeName, Long memberId) throws Exception {
        // 해당 콘텐츠 타입 이름과 연결된 숏폼 목록 조회
        List<ShortformDto> videos = formRepo.findVideosByContentTypeName(contentTypeName);

        if (videos.isEmpty()) {
            return videos; // 결과가 없으면 바로 반환
        }

        // 조회된 비디오 PK 목록 추출
        List<Integer> videoPks = videos.stream()
                                    .map(ShortformDto::getPk)
                                    .collect(Collectors.toList());

        // 해당 비디오들의 모든 콘텐츠 타입 매핑 정보를 한 번에 조회
        List<ShortformContentTypeMappingDto> mappings = formRepo.findShortformContentTypeMappingsByPks(videoPks);

        // 매핑 정보를 Shortform PK 별로 그룹화
        Map<Integer, List<String>> contentTypeMap = mappings.stream()
            .collect(Collectors.groupingBy(
                ShortformContentTypeMappingDto::getShortformPk,
                Collectors.mapping(
                    ShortformContentTypeMappingDto::getContentTypeName,
                    Collectors.toList()
                )
            ));

         // 로그인한 사용자라면, 조회된 비디오들에 대한 좋아요 여부를 한 번에 조회
         Map<Integer, Boolean> favoriteStatusMap = new HashMap<>();
         if (memberId != null && !videoPks.isEmpty()) {
             List<Integer> favoritedPks = formRepo.findFavoritedShortformPksByMember(memberId, videoPks);
             for (Integer pk : favoritedPks) {
                 favoriteStatusMap.put(pk, true);
             }
         }


        // 각 비디오 DTO에 해당하는 콘텐츠 타입 목록 및 좋아요 여부 설정
        for (ShortformDto video : videos) {
            List<String> types = contentTypeMap.getOrDefault(video.getPk(), new ArrayList<>());
            video.setContentTypes(types);

            video.setFavoritedByCurrentUser(favoriteStatusMap.getOrDefault(video.getPk(), false)); // 좋아요 여부 설정
        }

        return videos;
    }

	
	// ⭐ 수정: 업데이트 시 작성자 검증을 위해 memberId 파라미터 추가 (선택 사항, 컨트롤러에서 이미 할 수도 있음)
    // 파일 관련 로직은 컨트롤러로 이동했으므로, 여기서는 DTO를 받아 DB만 업데이트
    @Transactional
    public int updateVideo(ShortformDto form, Long memberId) throws Exception {
        // 1. (선택적) 여기서도 작성자 검증 한번 더
        ShortformDto existingVideo = formRepo.selectVideo(form.getPk());
        if (existingVideo == null) {
            throw new RuntimeException("Video not found with pk: " + form.getPk());
        }
        // Member 테이블과 연동하여 실제 사용자 ID로 비교하는 것이 더 정확
        // 여기서는 author 이름으로 비교 (컨트롤러에서 이미 memberId로 사용자 가져왔다고 가정)
        // if (!existingVideo.getAuthor().equals(authStore.currentUser.name)) { // 실제 사용자 정보로 비교 필요
        // throw new IllegalAccessException("You are not authorized to update this video.");
        // }

        // 2. shortform 테이블 업데이트
        int updateCount = formRepo.updateVideo(form); // 이 시점에 form의 pk, title, content, videofile 사용
        if (updateCount == 0) {
            throw new RuntimeException("Failed to update shortform video in DB.");
        }

        // 3. shortform_to_contenttype 테이블 업데이트 (기존 매핑 삭제 후 새로 삽입)
        // 사용자가 한 개만 선택해서 보내므로, 해당 한 개의 ID를 사용
        Integer selectedContentTypeId = form.getSelectedContentTypeId();

        if (selectedContentTypeId != null) {
            // 기존 매핑 삭제
            formRepo.deleteShortformContentTypeMappings(form.getPk());
            // 새 매핑 삽입
            formRepo.insertShortformContentType(form.getPk(), selectedContentTypeId);
        } else {
            throw new IllegalArgumentException("콘텐츠 타입이 선택되지 않았습니다. (Update)");
        }
        return updateCount;
    }
    
    // ⭐ 추가: 업데이트/삭제 전 작성자 확인 및 정보 조회를 위한 메서드
    public ShortformDto selectVideoForUpdate(int pk, Long memberId) throws Exception {
        ShortformDto video = formRepo.selectVideo(pk);
        if (video == null) {
            log.warn("Video not found for update/delete: PK {}", pk);
            return null;
        }
        // TODO: video.getAuthor() (이름) 대신, video 테이블에 member_id 외래키를 두고
        // 해당 member_id와 전달받은 memberId를 비교하는 것이 훨씬 안전하고 정확합니다.
        // 현재는 author 이름으로만 비교하고 있으므로, 동명이인 문제가 있을 수 있고,
        // OAuth 사용자의 경우 이름이 바뀔 수도 있습니다.
        // 임시 방편: MemberService를 주입받아 memberId로 사용자 이름을 가져와서 비교
        // MemberDto currentUser = memberService.selectMemberById(memberId);
        // if (currentUser == null || !video.getAuthor().equals(currentUser.getName())) {
        //     log.warn("User {} is not authorized to modify video PK {} owned by {}", memberId, pk, video.getAuthor());
        //     throw new IllegalAccessException("수정/삭제 권한이 없습니다."); // 컨트롤러에서 403 등으로 변환
        // }
        return video; // 권한 있으면 비디오 정보 반환
    }

	
    @Transactional
    public int deleteVideo(int pk, Long memberId) throws Exception {
        // 작성자 검증 로직은 selectVideoForUpdate에서 이미 수행했다고 가정하거나, 여기서도 수행
        // ShortformDto videoToDelete = selectVideoForUpdate(pk, memberId);
        // if (videoToDelete == null) { // 이미 위에서 호출했다면 중복
        //     return 0; // 삭제할 비디오가 없거나 권한 없음
        // }

        // shortform_to_contenttype 테이블에서 관련 매핑 먼저 삭제 (DB에서 CASCADE 설정 안했다면)
        // formRepo.deleteShortformContentTypeMappings(pk); // 이미 CASCADE ON DELETE로 설정됨

        // member_favorite_shortform 테이블에서 관련 좋아요 먼저 삭제 (DB에서 CASCADE 설정 안했다면)
        // formRepo.deleteAllFavoritesForShortform(pk); // 이미 CASCADE ON DELETE로 설정됨

        int cnt = formRepo.deleteVideo(pk);
        if (cnt == 0) {
            log.warn("Failed to delete video from DB or no video found with PK {}", pk);
        }
        return cnt;
    }
	
	public int incrementViewCount(int pk) throws Exception {
        int cnt = formRepo.incrementViewCount(pk);
        return cnt;
    }
	
	// --- 좋아요 관련 서비스 메서드 추가 ---
	@Transactional
    public boolean toggleFavorite(Long memberId, int shortformPk) throws Exception {
        // memberId가 null인 경우는 컨트롤러 레벨에서 이미 처리되었거나,
        // SecurityConfig에 의해 인증된 사용자만 접근 가능하므로 여기서는 memberId가 null이 아니라고 가정.
        // 만약을 위해 한번 더 체크할 수 있음:
        if (memberId == null) {
            log.warn("toggleFavorite 호출 시 memberId가 null입니다. shortformPk: {}", shortformPk);
            throw new IllegalArgumentException("사용자 ID가 없습니다. 로그인이 필요합니다."); // 또는 다른 적절한 예외
        }

        boolean isFavorited = formRepo.isFavorite(memberId, shortformPk) > 0;
        if (isFavorited) {
            formRepo.removeFavorite(memberId, shortformPk);
            formRepo.decrementFavoriteCount(shortformPk);
            log.info("사용자 ID {} 가 비디오 PK {} 의 좋아요를 취소했습니다.", memberId, shortformPk);
            return false;
        } else {
            formRepo.addFavorite(memberId, shortformPk);
            formRepo.incrementFavoriteCount(shortformPk);
            log.info("사용자 ID {} 가 비디오 PK {} 를 좋아합니다.", memberId, shortformPk);
            return true;
        }
    }

    public int getFavoriteCount(int shortformPk) throws Exception {
        Integer count = formRepo.getFavoriteCount(shortformPk);
        return count != null ? count : 0; // null일 경우 0 반환
    }
}
