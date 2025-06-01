package com.ssafy.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.dto.MemberDto;
import com.ssafy.dto.OAuth2MemberDto;
import com.ssafy.dto.RefreshTokenDto;
import com.ssafy.security.CustomUserDetails;
import com.ssafy.security.jwt.JwtTokenProvider;
import com.ssafy.service.MemberService;
import com.ssafy.service.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth/member")
@RequiredArgsConstructor
public class MemberRestController {
	
	private final MemberService service;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	
	
	@lombok.Data
    static class LoginRequest {
        private String email;
        private String pw;
    }
	
	@PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
		try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPw())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtTokenProvider.createAccessToken(authentication);
            String refreshTokenValue = jwtTokenProvider.createRefreshToken(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            MemberDto memberInfo = userDetails.getMemberDto();

            if (memberInfo.getId() != null) { // MemberDto에 pk 필드가 있다고 가정 (DB의 id)
            	refreshTokenService.saveOrUpdateRefreshToken((long) memberInfo.getId(), refreshTokenValue);
            } else {
                log.warn("일반 로그인 후 Member ID (PK)가 없어 Refresh Token을 저장하지 못했습니다: email={}", memberInfo.getEmail());
            }
            
            memberInfo.setPw(null); // 비밀번호 정보 제거
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "로그인 성공");
            responseBody.put("accessToken", accessToken);
            responseBody.put("refreshToken", refreshTokenValue);
            responseBody.put("memberInfo", memberInfo);

            return ResponseEntity.ok(responseBody);
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user {}: Invalid credentials", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                               .body(Map.of("message", "로그인 실패: 이메일 또는 비밀번호가 올바르지 않습니다."));
        } catch (Exception e) {
            log.error("Login error for user {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(Map.of("message", "로그인 중 오류가 발생했습니다."));
        }
    }
	
	@PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String oldRefreshTokenValue = jwtTokenProvider.resolveToken(request); // 헤더에서 Refresh Token 추출 가정

        if (oldRefreshTokenValue == null ) { // validateToken은 만료 여부도 체크하므로, 여기서는 존재 유무만
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Refresh Token이 없습니다."));
        }

        // DB에서 Refresh Token 조회 및 유효성 검사
        Optional<RefreshTokenDto> storedTokenOpt = refreshTokenService.findByTokenValue(oldRefreshTokenValue);

        if (storedTokenOpt.isEmpty()) { // isExpired()는 service 내부에서 처리 (만료 시 삭제 후 empty 반환)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "유효하지 않거나 만료된 Refresh Token 입니다."));
        }

        // Refresh Token으로 새로운 Access Token 발급 (사용자 정보는 DB에서 다시 조회하지 않고 토큰에서 추출)
        // Refresh Token 자체는 만료되지 않았지만, 내부의 subject(email)가 유효한지 확인하는 로직은 JwtTokenProvider에 있음
        if (!jwtTokenProvider.validateToken(oldRefreshTokenValue)) { // 만료되지 않았지만 다른 이유로 유효하지 않은 경우 (예: 서명 불일치)
              refreshTokenService.deleteByTokenValue(oldRefreshTokenValue); // 안전하게 삭제
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Refresh Token 서명이 유효하지 않습니다."));
        }


        Authentication authentication = jwtTokenProvider.getAuthentication(oldRefreshTokenValue);
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);

        // (선택 사항) Refresh Token Rotation: 기존 Refresh Token 삭제 후 새 Refresh Token 발급 및 저장
        // refreshTokenService.deleteByTokenValue(oldRefreshTokenValue);
        // String newRefreshTokenValue = jwtTokenProvider.createRefreshToken(authentication);
        // refreshTokenService.saveOrUpdateRefreshToken(storedTokenOpt.get().getMemberId(), newRefreshTokenValue);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("accessToken", newAccessToken);
        // if (newRefreshTokenValue != null) responseBody.put("refreshToken", newRefreshTokenValue); // Rotation 시

        return ResponseEntity.ok(responseBody);
    }
	
	@PostMapping("/logout") // 세션 사용 안하므로 서버측에서 특별한 처리 불필요. 클라이언트가 토큰 삭제.
    public ResponseEntity<?> logout() {
        // SecurityContextHolder.clearContext(); // 필요하다면 컨텍스트 클리어
        log.info("Logout request received. Client should clear the token.");
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다. 클라이언트에서 토큰을 삭제해주세요."));
    }

	
	@GetMapping("/detail/{email}")
	public ResponseEntity<?> getMemberDetail(@PathVariable String email) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // 자신의 정보만 조회 가능하도록 하거나, 관리자 권한 체크
        if (!currentUsername.equals(email) /* && !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) */) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "권한이 없습니다."));
        }

        try {
            MemberDto member = service.selectMember(email);
            if (member == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "회원 정보를 찾을 수 없습니다."));
            }
            member.setPw(null);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            log.error("Error fetching member detail for {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "회원 정보 조회 중 오류 발생"));
        }
    }

	@PostMapping("/register")
    public ResponseEntity<?> insertMember(@RequestBody MemberDto memberDto) {
        try {
            // 이메일 중복 검사
            if (service.selectMember(memberDto.getEmail()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "이미 사용 중인 이메일입니다."));
            }
            service.insertMember(memberDto); // 서비스에서 비밀번호 해싱 및 provider, role 설정
            log.info("Member registered: {}", memberDto.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "회원가입 성공"));
        } catch (Exception e) {
            log.error("Registration error for {}: {}", memberDto.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "회원가입 중 오류 발생"));
        }
    }

	@PatchMapping("/update")
    public ResponseEntity<?> updateMember(@RequestBody MemberDto memberDto) {
        // TODO: 현재 로그인한 사용자가 자신의 정보만 수정 가능하도록 권한 검사 추가
        try {
            int cnt = service.updateMember(memberDto);
            if (cnt > 0) {
                log.info("Member updated: {}", memberDto.getEmail());
                return ResponseEntity.ok(Map.of("message", "수정 성공"));
            } else {
                log.warn("Member update failed or no changes for: {}", memberDto.getEmail());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "수정할 회원을 찾지 못했거나 변경된 내용이 없습니다."));
            }
        } catch (Exception e) {
            log.error("Update error for {}: {}", memberDto.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "수정 중 오류 발생"));
        }
    }

	
	@DeleteMapping("/delete/{email}") // PathVariable로 email 받는 것을 권장
    public ResponseEntity<?> deleteMember(@PathVariable String email) {
        // TODO: 현재 로그인한 사용자가 자신의 계정만 삭제 가능하도록 권한 검사 추가
        try {
            int cnt = service.deleteMember(email);
            if (cnt > 0) {
                log.info("Member deleted: {}", email);
                return ResponseEntity.ok(Map.of("message", "삭제 성공"));
            } else {
                log.warn("Member delete failed for: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "삭제할 회원을 찾지 못했습니다."));
            }
        } catch (Exception e) {
            log.error("Delete error for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "삭제 중 오류 발생"));
        }
    }
	
	// OAuth2
	// OAuth2 관련 엔드포인트는 기존과 유사하게 유지 (주로 CustomOAuth2UserService에서 처리)
    @PostMapping("/oauth2register")
    public ResponseEntity<?> insertOAuth2Member(@RequestBody OAuth2MemberDto member) {
        try {
        	service.insertOAuth2Member(member);
            log.info("OAuth2 member registered via API: {}", member.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "OAuth2 멤버 추가 성공"));
        } catch (Exception e) {
            log.error("OAuth2 registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "OAuth2 멤버 추가 중 오류 발생"));
        }
    }

}
