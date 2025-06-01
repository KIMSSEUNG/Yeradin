package com.ssafy.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; 

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.dto.MemberDto;
import com.ssafy.dto.contenttype.ContentTypeDto;
import com.ssafy.dto.shortform.ShortformDto;
import com.ssafy.service.MemberService;
import com.ssafy.service.ShortformService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth/video")
@RequiredArgsConstructor
public class ShortformRestController {
	private final ShortformService service;
	 private final MemberService memberService;

	@Value("${file.upload-dir}")
	private String uploadDir;

	// Utility method to get member PK (Long) from AuthenticationPrincipal
    private Long getMemberPkFromPrincipal(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof UserDetails) { // 일반 JWT 로그인 사용자
            // CustomUserDetails를 사용하고 있고, MemberDto를 통해 pk를 가져올 수 있다면:
            // if (principal instanceof CustomUserDetails) {
            //     MemberDto memberDto = ((CustomUserDetails) principal).getMemberDto();
            //     return memberDto != null ? memberDto.getPk().longValue() : null; // MemberDto.pk가 Integer이므로 longValue()
            // }
            // UserDetails.getUsername()이 이메일이라고 가정하고, 이메일로 Member PK 조회
            String email = ((UserDetails) principal).getUsername();
            try {
                MemberDto member = memberService.selectMemberByEmail(email); // MemberService에 email로 조회하는 메서드 필요
                return member != null ? member.getId().longValue() : null;
            } catch (Exception e) {
                log.error("Error fetching member by email from UserDetails: {}", email, e);
                return null;
            }
        } else if (principal instanceof OAuth2User) { // OAuth2 로그인 사용자
            // CustomOAuth2UserService에서 "userPk"로 저장한 Long 타입의 PK를 가져옴
            Object pkObj = ((OAuth2User) principal).getAttribute("userPk");
            if (pkObj instanceof Long) {
                return (Long) pkObj;
            } else if (pkObj instanceof Integer) { // 혹시 Integer로 저장되었다면 Long으로 변환
                return ((Integer) pkObj).longValue();
            }
            // 또는 email 기반으로 조회
            String email = ((OAuth2User) principal).getAttribute("email");
             try {
                MemberDto member = memberService.selectMemberByEmail(email);
                return member != null ? member.getId().longValue() : null;
            } catch (Exception e) {
                log.error("Error fetching member by email from OAuth2User: {}", email, e);
                return null;
            }
        }
        log.warn("Unknown principal type: {}", principal.getClass().getName());
        return null;
    }

    @GetMapping("/contenttypes")
    public ResponseEntity<List<ContentTypeDto>> getContentTypes() {
        try {
            List<ContentTypeDto> contentTypes = service.getAllContentTypes();
            return ResponseEntity.ok(contentTypes);
        } catch (Exception e) {
            log.error("콘텐츠 타입 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
	
    
    
	// CREATE
    @PostMapping
    public ResponseEntity<ShortformDto> insertVideo(
            @RequestParam("videofile") MultipartFile videofile,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            // @RequestParam("author") String author, // 작성자는 인증 정보에서 가져옴
            @RequestParam(value = "contentTypeId", required = false) Integer contentTypeId,
            @AuthenticationPrincipal Object principal) { // 인증된 사용자 정보 받기
        
        Long memberPk = getMemberPkFromPrincipal(principal);
        String authorName = "익명"; // 기본값
        String authorEmail = null;

        if (principal instanceof UserDetails) {
            authorEmail = ((UserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            authorEmail = ((OAuth2User) principal).getAttribute("email");
            // OAuth2User에서 이름을 가져올 수도 있음
            // authorName = ((OAuth2User) principal).getAttribute("name");
        }
        
        if (authorEmail != null) {
            try {
                MemberDto member = memberService.selectMember(authorEmail);
                if (member != null) {
                    authorName = member.getName(); // DB에 저장된 이름 사용
                }
            } catch (Exception e) {
                log.warn("작성자 이름 조회 실패: {}", authorEmail);
            }
        }


        if (memberPk == null && principal != null) { // principal은 있는데 pk를 못가져온 경우 (로깅/디버깅용)
             log.warn("Principal is present but could not extract member PK for video creation. Principal: {}", principal);
        }
        // 비인증 사용자의 업로드를 막으려면 SecurityConfig에서 이 엔드포인트에 인증을 요구해야 함
        // 또는 여기서 memberPk == null 이면 401 반환
        if (memberPk == null && principal != null) { // principal이 null이 아닌데 pk를 못가져왔다면 문제.
             log.warn("Could not determine author from principal for video upload. Principal type: {}", principal.getClass().getName());
             // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 또는 예외 발생
        } else if (principal == null) { // 인증 정보 자체가 없는 경우 (SecurityConfig에서 permitAll된 경우)
            log.warn("Attempting to upload video without authentication.");
            // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 인증 필요시
        }

        if (contentTypeId == null) {
            log.warn("contentTypeId is null for video upload.");
            return ResponseEntity.badRequest().body(null); // 또는 Map.of("error", "콘텐츠 타입을 선택해야 합니다.") 등
       }
        
        try {
            if (videofile.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = videofile.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            videofile.transferTo(filePath.toFile());
            String videofileDbPath = uniqueFilename;

            ShortformDto form = new ShortformDto();
            form.setTitle(title);
            form.setVideofile(videofileDbPath);
            form.setAuthor(authorName); // 인증된 사용자 이름으로 설정
            form.setContent(content);
            form.setSelectedContentTypeId(contentTypeId);

            int result = service.insertVideo(form, memberPk); // Service는 author를 그대로 받아서 저장
            if (result > 0) {
                log.info("비디오 업로드 성공: {}", form);
                ShortformDto createdVideo = service.selectVideo(form.getPk(), memberPk); // 생성된 비디오 정보 반환 시에도 memberPk 전달
                return ResponseEntity.status(HttpStatus.CREATED).body(createdVideo);
            } else {
                log.warn("비디오 업로드 실패 (DB 삽입 오류): {}", form);
                return ResponseEntity.badRequest().build();
            }
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            log.error("비디오 업로드 중 예외 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

	// READ - 단건 조회
    @GetMapping("/{pk}")
    public ResponseEntity<ShortformDto> selectVideo(
            @PathVariable int pk,
            @AuthenticationPrincipal Object principal) throws Exception { // principal은 null일 수 있음 (비로그인 사용자)
        Long memberPk = getMemberPkFromPrincipal(principal);
        ShortformDto videoDto = service.selectVideo(pk, memberPk); // memberPk가 null이면 서비스에서 알아서 처리
        if (videoDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(videoDto);
    }
    
    @GetMapping("/related")
    public ResponseEntity<List<ShortformDto>> getRelatedVideos(
            @RequestParam("contentTypeName") String contentTypeName, // 콘텐츠 타입 이름으로 받음
            @AuthenticationPrincipal Object principal) {

        Long memberPk = getMemberPkFromPrincipal(principal);

        // 관련 쇼츠는 로그인 없이도 볼 수 있게 허용하려면 SecurityConfig 수정 필요.
        // 현재는 /api/auth/video 아래에 있으므로 인증 필요.
        // 비인증 사용자도 관련 쇼츠를 볼 수 있게 하려면 이 엔드포인트를 /api/video 로 옮기거나,
        // SecurityConfig에서 이 경로만 permitAll 처리하고, memberId가 null일 경우
        // 서비스 단에서 좋아요 여부 체크를 스킵하도록 처리해야 함.
        // 여기서는 일단 인증된 사용자만 볼 수 있다고 가정합니다.
        if (memberPk == null) {
             log.warn("Related videos request: User PK not found. Principal: {}", principal);
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 또는 Map.of("error", "로그인이 필요합니다.")
        }

        try {
            // Service 메서드 호출 시 콘텐츠 타입 이름과 memberPk 전달
            List<ShortformDto> relatedVideos = service.selectVideosByContentTypeName(contentTypeName, memberPk);
            if (relatedVideos.isEmpty()) {
                // 관련 비디오가 없을 경우 204 No Content 또는 200 OK에 빈 리스트 반환
                 log.info("Related videos not found for type: {}", contentTypeName);
                return ResponseEntity.ok(relatedVideos); // 빈 리스트 반환
            }
             log.info("Found {} related videos for type: {}", relatedVideos.size(), contentTypeName);
            return ResponseEntity.ok(relatedVideos);

        } catch (Exception e) {
            log.error("관련 비디오 조회 중 오류 발생 (타입: {}): {}", contentTypeName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

	// READ - 전체 조회
	@GetMapping
	public ResponseEntity<List<ShortformDto>> selectAllVideo(@AuthenticationPrincipal Object principal) {
		Long memberPk = getMemberPkFromPrincipal(principal);
		try {
			List<ShortformDto> list = service.selectAllVideo(memberPk);
			return ResponseEntity.ok(list);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	// UPDATE
	@PutMapping("/{pk}")
    public ResponseEntity<ShortformDto> updateVideo(
            @PathVariable int pk,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "contentTypeId", required = true) Integer contentTypeId,
            @RequestParam(value = "videofile", required = false) MultipartFile videofile, // 파일은 선택 사항
            @AuthenticationPrincipal Object principal) throws Exception {

        Long memberPk = getMemberPkFromPrincipal(principal);
        if (memberPk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 1. 기존 비디오 정보 조회 (작성자 확인 및 기존 파일 정보 가져오기 위함)
        ShortformDto existingVideo = service.selectVideoForUpdate(pk, memberPk); // 작성자 검증 포함된 서비스 메서드 필요
        if (existingVideo == null) {
            // 비디오가 없거나 수정 권한이 없는 경우 (selectVideoForUpdate 내부에서 처리 가능)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 또는 FORBIDDEN
        }
        
        // 2. ShortformDto 객체 생성 또는 기존 객체 업데이트
        ShortformDto formToUpdate = new ShortformDto();
        formToUpdate.setPk(pk); // PK 설정
        formToUpdate.setTitle(title);
        formToUpdate.setContent(content);
        formToUpdate.setSelectedContentTypeId(contentTypeId);
        // author는 기존 작성자 유지 (또는 필요시 principal에서 가져와서 설정)
        formToUpdate.setAuthor(existingVideo.getAuthor()); 


        String videofileDbPath = existingVideo.getVideofile(); // 기본적으로 기존 파일 유지

        // 3. 새 파일이 업로드된 경우 처리
        if (videofile != null && !videofile.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // 기존 파일 삭제 (선택 사항, 필요하다면)
            if (existingVideo.getVideofile() != null && !existingVideo.getVideofile().isEmpty()) {
                try {
                    Path oldFilePath = uploadPath.resolve(existingVideo.getVideofile());
                    Files.deleteIfExists(oldFilePath);
                    log.info("Old video file deleted: {}", oldFilePath);
                } catch (IOException e) {
                    log.error("Failed to delete old video file: {}", existingVideo.getVideofile(), e);
                    // 파일 삭제 실패가 전체 업데이트 실패를 의미하지는 않을 수 있음 (비즈니스 로직에 따라 결정)
                }
            }

            String originalFilename = videofile.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            videofile.transferTo(filePath.toFile());
            videofileDbPath = uniqueFilename; // DB에 저장될 새 파일 경로
        }
        formToUpdate.setVideofile(videofileDbPath); // DTO에 최종 파일 경로 설정

        // 4. 서비스 호출하여 업데이트
        // service.updateVideo 메서드는 이제 파일 처리 로직 없이 DTO만 받아서 DB 업데이트
        int result = service.updateVideo(formToUpdate, memberPk); // memberPk로 권한 한번 더 확인 가능

        if (result > 0) {
            ShortformDto updatedVideo = service.selectVideo(pk, memberPk); // 업데이트된 정보 다시 조회
            return ResponseEntity.ok(updatedVideo);
        } else {
            log.warn("비디오 업데이트 실패 (DB 업데이트 오류): {}", formToUpdate);
            return ResponseEntity.badRequest().build(); // 또는 INTERNAL_SERVER_ERROR
        }
    }
	// DELETE
    @DeleteMapping("/{pk}")
    public ResponseEntity<Void> delete(@PathVariable int pk, @AuthenticationPrincipal Object principal) throws Exception {
        Long memberPk = getMemberPkFromPrincipal(principal);
        if (memberPk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // ⭐ 서비스 단에서 pk 비디오의 작성자와 memberPk가 일치하는지 확인하는 로직 필요
        // 예: service.deleteVideo(pk, memberPk);
        // service.deleteVideo(pk); // 현재 서비스는 작성자 검증 로직 없음
        
        // 파일 시스템에서 실제 비디오 파일도 삭제하는 로직 추가
        ShortformDto videoToDelete = service.selectVideoForUpdate(pk, memberPk); // 작성자 검증 포함
        if (videoToDelete == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 비디오 없거나 권한 없음
        }

        // DB에서 레코드 삭제
        int result = service.deleteVideo(pk, memberPk); // 작성자 검증 포함된 삭제 메서드

        if (result > 0) {
            // 파일 시스템에서 파일 삭제
            if (videoToDelete.getVideofile() != null && !videoToDelete.getVideofile().isEmpty()) {
                try {
                    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                    Path filePath = uploadPath.resolve(videoToDelete.getVideofile());
                    Files.deleteIfExists(filePath);
                    log.info("Video file deleted from filesystem: {}", filePath);
                } catch (IOException e) {
                    log.error("Failed to delete video file from filesystem: {}", videoToDelete.getVideofile(), e);
                    // 파일 삭제 실패 시 로깅만 하고 DB 삭제는 성공으로 처리할 수 있음
                }
            }
            return ResponseEntity.ok().build();
        } else {
            log.warn("비디오 삭제 실패 (DB 삭제 오류 또는 권한 없음): PK {}", pk);
            return ResponseEntity.badRequest().build(); // 또는 FORBIDDEN, INTERNAL_SERVER_ERROR
        }
    }
	
	@PutMapping("/{pk}/view")
    public ResponseEntity<Void> incrementView(@PathVariable int pk) {
        try {
            service.incrementViewCount(pk);
            return ResponseEntity.ok().build(); // 성공 시 200 OK
        } catch (Exception e) { // 구체적인 예외 처리 권장 (예: VideoNotFoundException)
            // log.error("Error incrementing view count for pk {}: {}", pk, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
	
	// --- 좋아요 토글 API ---
    @PostMapping("/{pk}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable int pk,
            @AuthenticationPrincipal Object principal) { // 인증된 사용자만 이 API 사용 가능하도록 SecurityConfig에서 설정 필요

        Long memberPk = getMemberPkFromPrincipal(principal);

        if (memberPk == null) {
            // SecurityConfig에서 /api/auth/** 경로는 인증을 요구하므로, 여기까지 오면 principal은 null이 아닐 것으로 예상.
            // 다만, getMemberPkFromPrincipal 내부에서 PK 추출 실패 시 null 반환 가능
            log.warn("좋아요 토글 요청: 사용자 PK를 확인할 수 없습니다. Principal: {}", principal);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인 정보가 유효하지 않습니다."));
        }

        try {
            boolean isNowFavorited = service.toggleFavorite(memberPk, pk); // 서비스에 Long 타입 memberPk 전달
            int currentFavoriteCount = service.getFavoriteCount(pk);

            Map<String, Object> response = new HashMap<>();
            response.put("favoritedByCurrentUser", isNowFavorited);
            response.put("favoriteCount", currentFavoriteCount);
            
            log.info("비디오 PK {} 좋아요 토글 결과: 사용자 PK {}, 현재 좋아요 상태 {}, 총 좋아요 수 {}", pk, memberPk, isNowFavorited, currentFavoriteCount);
            return ResponseEntity.ok(response);
        } catch (IllegalAccessException e) { // Service에서 던지는 예외 (여기서는 사용 안함)
             log.warn("좋아요 토글 실패: 비디오 PK {}, 사용자 PK {}", pk, memberPk, e);
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("비디오 PK {} 좋아요 토글 중 오류 발생 (사용자 PK {}): {}", pk, memberPk, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "좋아요 처리 중 오류가 발생했습니다."));
        }
    }
}
