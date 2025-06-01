package com.ssafy.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ssafy.dto.board.BoardContenRegistDto;
import com.ssafy.dto.board.BoardContentDto;
import com.ssafy.dto.board.BoardDetailDto;
import com.ssafy.dto.board.BoardPreviewDto;
import com.ssafy.dto.board.BoardRegistDto;
import com.ssafy.dto.board.BoardSearchDto;
import com.ssafy.dto.board.BoardUpdateDto;
import com.ssafy.dto.board.ImageDto;
import com.ssafy.repository.BoardRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class BoardService {
	private final BoardRepository boardRepo;
	@Value("${spring.servlet.multipart.location}")
    String filePath;
	
	
//	public List<BoardPreviewDto> findAll() throws SQLException, ClassNotFoundException{
//		return boardRepo.findAll();
//	}
	
	public List<BoardPreviewDto> getBoards(int page, int size) {
		int offset = (page - 1) * size;
		
		Map<String, Object> param = new HashMap<>();
		param.put("offset", offset);
		param.put("size", size);
		
		return boardRepo.findByBoards(param);
	}
	
	public int getBoardCount() {
		return boardRepo.countBoards();
	}
	
	public int countFilter(BoardSearchDto dto) {
		return boardRepo.countFilter(dto);
	}
	
	public List<BoardPreviewDto> search(int page , int size ,String category, String keyword) throws SQLException, ClassNotFoundException {
		// TODO Auto-generated method stub
		
		Map<String, Object> param = new HashMap<>();
		int offset = (page - 1) * size;
		param.put("offset", offset);
		param.put("size", size);
		param.put("category", category);
		param.put("keyword", keyword);
		
		return boardRepo.search(param);
	}
	
	

	public BoardDetailDto boardDetail(String id) {
		return boardRepo.findDetail(id);
	}
	
	public void boardRegist(BoardRegistDto dto) throws IOException{
		Path uuidFolderPath=null;
		try {
        	
        	if(dto.getImages().length == 0) {
        		//insert board
        		System.out.println(dto.getContent());
        		dto.setContentPriview(createContentPreview(dto.getContent()));
        		//contentPrive 제작
        		boardRepo.save(dto);
        	}
        	else {
        		List<String> uploadedUrls = new ArrayList<>();
            	
                Path uploadPath = Paths.get(filePath);
                // uploads 폴더가 없으면 생성
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                // uploads 폴더 안에 uuid 폴더 만들기
                // 2. UUID로 하위 폴더 생성
                String uuidFolder = UUID.randomUUID().toString();
                uuidFolderPath = uploadPath.resolve(uuidFolder); // uploads/uuid
                Files.createDirectories(uuidFolderPath);
                
                List<ImageDto> imgList = new ArrayList<>();
        		dto.setImgForderPath(uuidFolderPath.toString());
            	MultipartFile[] images = dto.getImages();
            	for (int i = 0; i < images.length; i++) {
                    MultipartFile image = images[i];
                    String originImgName= imageNameChange(image.getOriginalFilename());
                    String storeFileName = UUID.randomUUID() + "_" + originImgName;
                    Path targetPath = uuidFolderPath.resolve(storeFileName); //경로 + 파일명 결합 = 실제 저장할 위치

                    image.transferTo(targetPath.toFile()); //사용자가 업로드한 이미지 파일을 해당 경로에 저장

                    String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    	    .path("/images/")
                    	    .path(uuidFolder + "/")
                    	    .path(storeFileName)
                    	    .toUriString();
                    // 외부에 나의 디렉터리 경로를 설정하는 것은 보안 이슈에 걸릴 수 있음으로, images 라는 임의 url로 설정한다.
                    if(dto.getThumbnailUrl()==null)dto.setThumbnailUrl(imageUrl);
                    uploadedUrls.add(imageUrl); // Content에 들어갈 토큰을 uploadUrls명으로 바꿔주기 위해서 저장
                    imgList.add(ImageDto.builder()
                    		.boardId(dto.getId()).originalName(originImgName)
                    		.storeName(storeFileName).build());
                }
            	
            	//썸네일 이미지 저장
            	
                // content의 토큰을 이미지 URL로 치환
            	// 해당하는 이미지는 src==로 만들어지고, 브라우저에서 해당 경로로 이미지를 받기 위해ㅔ HTTP 요청을 보낸다. (정적 리소스 설정)
                String content = dto.getContent();
                //content Priview 내용 저장
                dto.setContentPriview(createContentPreview(content));
                for (int i = 0; i < uploadedUrls.size(); i++) {
                	content = content.replace("__IMAGE_"+i+"__", "<img src='" + uploadedUrls.get(i) + "' class='inline-image'>");
                }
                dto.setContent(content);
                System.out.println(content);
                //insertBoard
                boardRepo.save(dto);
                //insertImg
        		boardRepo.imageSave(imgList);
        	}
        	
        } catch (Exception e) {
	   		 //이미지 폴더 삭제
        	if (uuidFolderPath!=null && Files.exists(uuidFolderPath)){
	   			 Files.walk(uuidFolderPath)                // 하위 모든 파일/디렉터리 탐색
	   	         .sorted(Comparator.reverseOrder()) // 하위 → 상위 순서로 삭제
	   	         .map(Path::toFile)
	   	         .forEach(File::delete);
	   		 }
        	e.printStackTrace();
        	throw new RuntimeException();
        }
	}
	
	private String imageNameChange(String name){
        String cleanedName = name
            .replaceAll("[^a-zA-Z0-9.\\-]", "_"); // 안전 문자만 허용
        return cleanedName;
    }
    
    private String createContentPreview(String content) {
        if (content == null) return null;

        // 1. 이미지 태그 플레이스홀더 제거
        String textOnly = content.replaceAll("__IMAGE_\\d+__", "[사진]");

        // 2. HTML 태그 제거 (replace 후에도 <br> 남을 수 있으니 제거)
        textOnly = textOnly.replaceAll("<[^>]*>", "").trim();

        // 3. 공백 제거 후 길이 검사
        if (textOnly.isEmpty()) return null;

        // 4. 30자 이하라면 그대로, 초과라면 잘라서 반환
        return textOnly.length() <= 30 ? textOnly : textOnly.substring(0, 30);
    }
	
    public void boardDelete(String id) throws IOException {
        // DB에서 이미지 폴더 경로 조회
        String forderPath = boardRepo.findByForderPath(id);

        // forderPath가 null이 아니고, 비어있지 않은 경우에만 폴더 삭제 시도
        if (forderPath != null && !forderPath.isEmpty()) {
            Path directory = Paths.get(forderPath);

            // 이미지 폴더 존재 여부 확인 후 삭제
            if (Files.exists(directory)) {
                try {
                    Files.walk(directory)                // 하위 모든 파일/디렉터리 탐색
                         .sorted(Comparator.reverseOrder()) // 하위 → 상위 순서로 삭제
                         .map(Path::toFile)
                         .forEach(File::delete);
                    System.out.println("이미지 폴더 삭제 완료: " + directory.toString()); // 로그 추가 (선택 사항)
                } catch (IOException e) {
                    // 폴더 삭제 중 예외 발생 시 로그 남기고 계속 진행 (DB 삭제는 해야 하므로)
                    System.err.println("이미지 폴더 삭제 중 오류 발생: " + directory.toString() + " - " + e.getMessage());
                    // 필요하다면 여기서 더 구체적인 예외 처리를 할 수 있습니다.
                    // 예를 들어, throw new RuntimeException("이미지 폴더 삭제 실패", e); 등으로 처리할 수도 있지만,
                    // DB 레코드 삭제는 시도하는 것이 좋을 수 있습니다.
                }
            } else {
                System.out.println("삭제할 이미지 폴더가 존재하지 않습니다: " + forderPath); // 로그 추가 (선택 사항)
            }
        } else {
            System.out.println("해당 게시글(" + id + ")에 연결된 이미지 폴더 경로가 없습니다 (DB 값: null 또는 empty)."); // 로그 추가 (선택 사항)
        }

        // DB 데이터 삭제
        boardRepo.delete(id);
    }

	public void boardUpdate(String id ,BoardRegistDto dto) throws IOException {
		//delete
		boardDelete(id);
		//regist
		boardRegist(dto);
	}

	public void boardCommentRegist(BoardContenRegistDto dto) {
		// TODO Auto-generated method stub		
		boardRepo.contentSave(dto);

	}

	public List<BoardContentDto> findByBoardComments(String boardId) {
		// TODO Auto-generated method stub
		return boardRepo.findByBoardComments(boardId);
	}

	public void deleteComment(int id) {
		// TODO Auto-generated method stub
		boardRepo.deleteComment(id);
	}



	
	
}
