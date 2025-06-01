package com.ssafy.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.dto.board.BoardContenRegistDto;
import com.ssafy.dto.board.BoardContentDto;
import com.ssafy.dto.board.BoardDetailDto;
import com.ssafy.dto.board.BoardPreviewDto;
import com.ssafy.dto.board.BoardRegistDto;
import com.ssafy.dto.board.BoardSearchDto;
import com.ssafy.dto.board.BoardUpdateDto;
import com.ssafy.service.BoardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/board")
@RequiredArgsConstructor
public class BoardRestController {
	
	private final BoardService service;

//	@GetMapping
//	private List<BoardPreviewDto> findAll() throws Exception {
//		List<BoardPreviewDto> all = service.findAll();
//		return all;
//	}
	
	@GetMapping("")
	public ResponseEntity<?> getBoards(
	    @RequestParam(defaultValue = "1") int page,
	    @RequestParam(defaultValue = "5") int size
	) {
		System.out.println("요청 : Board"+" "+page+" "+size);
		List<BoardPreviewDto> boards = service.getBoards(page,size);
		System.out.println(boards.toString());
	    return ResponseEntity.ok(boards);
	}
	
	@GetMapping("/count")
	public ResponseEntity<?> getCount( ) {
		System.out.println("요청 : 카운트");
		int count = service.getBoardCount();
		System.out.println("board size : "+count);
		return ResponseEntity.ok(count);
	}
	
	@GetMapping("/filter/count")
	public ResponseEntity<?> getFilterCount(@ModelAttribute BoardSearchDto dto) {
		System.out.println("요청 : 필터 카운트");
		System.out.println(dto.toString());
		int count = service.countFilter(dto);
		return ResponseEntity.ok(count);
	}
	
	@GetMapping("/filter")
	private List<BoardPreviewDto> findFilter(
			@RequestParam(defaultValue = "1") int page,
		    @RequestParam(defaultValue = "5") int size,
			@ModelAttribute BoardSearchDto dto
			) throws Exception {
		System.out.println("요청 : Boardfilter"+" "+page+" "+size);
		List<BoardPreviewDto> all = service.search(page,size,dto.getCategory() , dto.getKeyword());
		return all;
	}
	

	@GetMapping("/{id}")
	private ResponseEntity boardDetail(@PathVariable String id) throws Exception {
		BoardDetailDto dto = service.boardDetail(id);
		return ResponseEntity.status(200).body(dto);
	}
	
	@PostMapping
	private ResponseEntity regist(@ModelAttribute BoardRegistDto dto) throws Exception {
        try {
        	String uuid = UUID.randomUUID().toString();
        	dto.setId(uuid);
        	service.boardRegist(dto);
            return ResponseEntity.status(200).build();

        } catch (IOException e) {
        	e.printStackTrace();
        	return ResponseEntity.status(400).build();
        }
	}
	
	@PostMapping("/comment")
	private ResponseEntity commentRegist(@RequestBody BoardContenRegistDto dto) throws Exception {
    	System.out.println(dto.toString());
		service.boardCommentRegist(dto);
        return ResponseEntity.status(200).build();
	}
	
	@GetMapping("/comment")
	private ResponseEntity getComments(@RequestParam String boardId) {
		List<BoardContentDto> dtos =  service.findByBoardComments(boardId);
        return ResponseEntity.status(200).body(dtos);
	}
	
	@DeleteMapping("/comment/{id}")
	private ResponseEntity deleteComment(@PathVariable int id) {
		service.deleteComment(id);
        return ResponseEntity.status(200).build();
	}

	
	@PutMapping("/{id}")
	private ResponseEntity update(@PathVariable String id,@ModelAttribute BoardRegistDto dto){
		try {
			service.boardUpdate(id,dto);
			return ResponseEntity.status(200).build();
		}catch(IOException e) {
			e.printStackTrace();
			return ResponseEntity.status(400).build();
		}

	}
	
	@DeleteMapping("/{id}")
	private ResponseEntity delete(@PathVariable String id) throws Exception {
		try {
			service.boardDelete(id);
			return ResponseEntity.status(200).build();
		}catch(IOException e) {
			e.printStackTrace();
			return ResponseEntity.status(400).build();
		}
		
	}
	
//	//파일 업로드
//    @PostMapping("/fileupload")
//    public ResponseEntity fileUpload(@RequestParam MultipartFile file) {
//        File localFile = new File(filePath, file.getOriginalFilename());
//        try {
//            //path 저장
//        	//contentService.savePath(file.getOriginalFilename().split("\\.")[0]);
//			saveAsZip(file , Paths.get(filePath) ,file.getOriginalFilename().split("\\.")[0]);
//        	//file.transferTo(localFile);
//            return ResponseEntity.status(200).build();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(400).build();
//        }
//       
//    }
//    
//    //게시판 읽어오기
//    @GetMapping("/{fileName:.+}")
//	public ResponseEntity<Resource> serveFile(@PathVariable String fileName) throws IOException {
//		// 1. ZIP 파일 존재 확인
//		Path zipPath = Paths.get(filePath,fileName+".zip");
//		System.out.println("ZIP 경로: " + zipPath.toAbsolutePath());
//		if (!Files.exists(zipPath)) {
//			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//		}
//
//		// 2. temp 디렉토리 생성
//		Path tempRootDir = Paths.get(filePath, "temp-unzip");
//		String tempFolderName = UUID.randomUUID().toString();
//		Path tempDir = tempRootDir.resolve(tempFolderName);
//		Files.createDirectories(tempDir);
//
//		// 3. ZIP에서 원하는 파일만 추출
//		boolean found = false;
//		Path extractedFile = tempDir.resolve(fileName + ".jpg");
//		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
//			ZipEntry entry;
//			while ((entry = zis.getNextEntry()) != null) {
//				if (entry.getName().equals(fileName + ".jpg")) {
//					Files.copy(zis, extractedFile, StandardCopyOption.REPLACE_EXISTING);
//					found = true;
//					break;
//				}
//			}
//		}
//
//		if (!found) {
//			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//		}
//
//		// 4. 응답 준비
//		Resource resource = new FileSystemResource(extractedFile.toFile());
//
//		// 5. 일정 시간 후 temp 디렉토리 삭제 (비동기)
//		Executors.newSingleThreadScheduledExecutor().schedule(() -> {
//			try {
//				Files.walk(tempDir)
//						.sorted(Comparator.reverseOrder())
//						.map(Path::toFile)
//						.forEach(File::delete);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}, 10, TimeUnit.SECONDS); // 10초 후 삭제
//
//
//
//		// 6. 파일 응답
//		return ResponseEntity.ok()
//				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + ".jpg\"")
//				.contentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM))
//				.body(resource);
//	}
//
//    
//    
//    //받아온 이미지 파일 알집화
//    public void saveAsZip(MultipartFile file, Path uploadPath, String zipFileName) throws IOException {
//		// 저장 경로 생성
//		if (!Files.exists(uploadPath)) {
//			Files.createDirectories(uploadPath);
//		}
//
//		// 압축파일 전체 경로 (예: uploads/file.zip)
//		if (!zipFileName.endsWith(".zip")) {
//			zipFileName += ".zip";
//		}
//		Path zipPath = uploadPath.resolve(zipFileName);
//
//
//		// ZipOutputStream으로 압축 파일 생성
//		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
//			// 압축 내부에 들어갈 파일 이름
//			String entryName = file.getOriginalFilename();
//			zos.putNextEntry(new ZipEntry(entryName));
//			zos.write(file.getBytes());
//			zos.closeEntry();
//		}
//	}
	
}
