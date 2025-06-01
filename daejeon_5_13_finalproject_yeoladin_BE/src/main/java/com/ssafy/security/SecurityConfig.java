package com.ssafy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.security.jwt.JwtAuthenticationFilter;
import com.ssafy.security.jwt.JwtTokenProvider;
import com.ssafy.service.MemberService; // MemberService 경로에 맞게 수정
import com.ssafy.service.RefreshTokenService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections; // JwtTokenProvider 사용 시 필요할 수 있음
import java.util.HashMap;
import java.util.Map;

// JwtTokenProvider를 사용한다면 import 추가
// import com.ssafy.util.JwtTokenProvider;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;


	@Autowired
	private final JwtTokenProvider jwtTokenProvider;

	@Autowired
	private final RefreshTokenService refreshTokenService;

	// JWT 토큰 프로바이더를 사용한다면 주입 (이 예제에서는 직접 문자열 사용)
	// @Autowired
	// private JwtTokenProvider jwtTokenProvider;
	@Autowired
	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, JwtTokenProvider jwtTokenProvider,
			RefreshTokenService refreshTokenService
	/* , RefreshTokenRepository refreshTokenRepository */) {
		this.customOAuth2UserService = customOAuth2UserService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenService = refreshTokenService; // 할당
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
				.csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
				.formLogin(login -> login.disable()) // 로그인 폼 비활성화 (OAuth2 사용 시 자체 폼 불필요)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(login -> login.disable()) // 일반 폼 로그인 사용 안함 (REST API)
				.httpBasic(basic -> basic.disable()) // HTTP Basic 비활성화
				.authorizeHttpRequests(auth -> auth
						// 1순위: 모든 사용자가 접근 가능한 정적 리소스 및 특정 페이지
						.requestMatchers("/api/auth/board","/api/auth/tripMap/content","/api/auth/board/comment" , "/api/auth/tripMap/filter","/api/auth/tripMap/detail","/api/auth/tripMap/ai" ).permitAll()
						.requestMatchers("/", "/login", "/register", "/favicon.ico").permitAll()
						.requestMatchers("/css/**", "/js/**", "/images/**", "/videos/**").permitAll() // 비디오 경로 포함

						// 2순위: OAuth2 관련 경로 (인증 과정 자체는 허용)
						.requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()

						// 3순위: 인증 없이 접근 가능한 API (회원가입, 로그인 등)
						.requestMatchers("/api/auth/member/login", "/api/auth/member/register").permitAll()
						.requestMatchers("/api/auth/tripMap/content", "/api/auth/tripMap/filter").permitAll()
						.requestMatchers("/api/main/popular-shortforms").permitAll()
						.requestMatchers("/videos/**").permitAll() // 비디오 파일 접근 허용
					    // .requestMatchers("/thumbnails/**").permitAll() // 썸네일 파일 접근 허용 (만약 사용한다면)

						// 4순위: 그 외 모든 요청은 인증 필요
						.anyRequest().authenticated())
				.oauth2Login(oauth2Login -> oauth2Login
						// .loginPage("/login") // 사용자 정의 로그인 페이지 (선택 사항)
						.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(customOAuth2UserService) // 사용자
																													// 정보
																													// 처리
																													// 서비스
																													// 등록
						).successHandler(authenticationSuccessHandler()) // 로그인 성공 후 처리 핸들러
				// .defaultSuccessUrl("http://localhost:5173/oauth/redirect", true) //
				// successHandler 사용 시 주석 처리
				// .failureUrl("/loginFailure") // 로그인 실패 시 리디렉션 (선택 사항)
						// 개발용 주소 "http://localhost:5173/"
				).logout(logout -> logout.logoutSuccessUrl("http://192.168.205.56:5173/") // 로그아웃 성공 시 프론트엔드 홈으로 리디렉션
						.permitAll())
				// JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 전에 추가
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
						UsernamePasswordAuthenticationFilter.class);
		;
		return http.build();
	}

	@Bean
	public AuthenticationSuccessHandler authenticationSuccessHandler() {
		return (request, response, authentication) -> {
			OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
			
			String accessToken = jwtTokenProvider.createAccessToken(authentication);
			String refreshTokenValue = jwtTokenProvider.createRefreshToken(authentication); // 'Value' 추가하여 명확히

			// CustomOAuth2UserService에서 attributes에 저장한 사용자 정보 추출
	        // CustomOAuth2UserService에서 customAttributes.put("name", finalMemberDto.getName()); 와 같이 저장했어야 함
	        // CustomOAuth2UserService에서 customAttributes.put("email", finalMemberDto.getEmail()); 와 같이 저장했어야 함
	        String name = oAuth2User.getAttribute("name");
	        String email = oAuth2User.getAttribute("email"); // oAuth2User.getName() 또는 oAuth2User.getAttribute("email") 둘 다 가능할 수 있음

	        // Refresh Token 저장 로직 (memberId 필요)
	        // CustomOAuth2UserService에서 "userPk"로 저장한 PK를 가져옴
	        Object userPkObj = oAuth2User.getAttribute("userPk"); // getAttribute 반환 타입이 Object이므로 Long으로 캐스팅 필요
	        log.info("Retrieved userPk from OAuth2User attributes: {}", userPkObj);
	        Long memberPk = null;

            if (userPkObj instanceof Integer) {
                memberPk = ((Integer) userPkObj).longValue(); // Integer를 long으로 변환 후 Long으로 박싱
            } else if (userPkObj instanceof Long) {
                memberPk = (Long) userPkObj; // 혹시 Long으로 저장된 경우
            } else if (userPkObj != null) {
                // 예상치 못한 타입이 들어온 경우 로그를 남기고 에러 처리를 고려할 수 있습니다.
                log.warn("userPk attribute is of an unexpected type: {} for user: {}",
                        userPkObj.getClass().getName(), email);
                // 이 경우, email로 DB에서 사용자 정보를 다시 조회하여 pk를 얻는 것을 시도해볼 수 있습니다.
                // (아래 else 블록의 로직과 유사)
            }
	        if (memberPk != null) {
	            refreshTokenService.saveOrUpdateRefreshToken(memberPk, refreshTokenValue);
	        } else {
	            log.warn("OAuth2 로그인 후 Member PK를 찾을 수 없어 Refresh Token을 저장하지 못했습니다. OAuth2User attributes: {}", oAuth2User.getAttributes());
	            // PK를 찾지 못한 경우, email 기반으로 DB에서 조회하여 PK를 가져오는 로직 추가 가능
	            // 예: MemberDto member = memberService.selectMember(email); if (member != null) memberPk = member.getPk();
	        }
	        
	        log.info("[SecurityConfig] Resolved Member PK for redirect: {}", memberPk);

	        // 프론트엔드로 리디렉션할 URL 구성
	        // 개발용 주소
	         String targetUrl = "http://localhost:5173/login";
//	        String targetUrl = "http://192.168.205.56:5173/login"; // 또는 OAuth 콜백을 처리하는 프론트엔드의 특정 라우트
	        targetUrl += "?oauth_success=true";
	        targetUrl += "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8.toString());
	        targetUrl += "&refreshToken=" + URLEncoder.encode(refreshTokenValue, StandardCharsets.UTF_8.toString());

	        // name과 email을 URL 파라미터로 추가
	        if (name != null && !name.isEmpty()) {
	            targetUrl += "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8.toString());
	        }
	        if (email != null && !email.isEmpty()) {
	            targetUrl += "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
	        }
	        
	        if (memberPk != null) {
	            targetUrl += "&id=" + URLEncoder.encode(String.valueOf(memberPk), StandardCharsets.UTF_8.toString()); // "id"라는 이름으로 전달
	        } else {
	            log.warn("Member PK is null. Cannot append 'id' to redirect URL for user: {}", email);
	            // PK가 null인 경우에 대한 예외 처리 또는 로깅 강화
	        }
	        log.debug("OAuth2 login success, redirecting to: {}", targetUrl);
	        response.sendRedirect(targetUrl);
		};
	}

	private Long getMemberIdFromOAuth2User(OAuth2User oAuth2User) {
		// CustomOAuth2UserService에서 DB 저장 후 MemberDto의 pk(id)를 oAuth2User attributes에
		// 넣어줬다고 가정
		// 예: attributes.put("memberId", savedMember.getPk());
		Object memberIdObj = oAuth2User.getAttribute("memberId");
		if (memberIdObj instanceof Long) {
			return (Long) memberIdObj;
		} else if (memberIdObj instanceof Integer) {
			return ((Integer) memberIdObj).longValue();
		}
		// 또는 email을 기반으로 DB에서 조회
		// String email = oAuth2User.getAttribute("email");
		// if (email != null) {
		// try {
		// MemberDto member = memberService.selectMember(email); // memberService 주입 필요
		// return member != null ? member.getPk() : null;
		// } catch (Exception e) { log.error("Error fetching member by email", e);
		// return null; }
		// }
		return null;
	}

	@Bean
	public LogoutSuccessHandler jwtLogoutSuccessHandler() {
		return (request, response, authentication) -> {
			// 클라이언트 측에서 Access Token과 Refresh Token을 삭제하도록 유도
			// 서버 측에서는 Refresh Token이 저장되어 있다면 삭제 처리 (선택 사항, 보안 강화)
			// if (authentication != null && authentication.getName() != null) {
			// refreshTokenRepository.deleteByKey(authentication.getName());
			// }
			if (authentication != null && authentication.getName() != null) {
				// email을 통해 Member ID 조회 후 Refresh Token 삭제
				try {
					// MemberDto member = memberService.selectMember(authentication.getName()); //
					// MemberService 주입 필요
					// if (member != null && member.getPk() != null) {
					// refreshTokenService.deleteByMemberId(member.getPk());
					// }
					// 또는 클라이언트가 Refresh Token을 헤더나 바디로 보내면 그걸로 삭제
					String refreshTokenValueFromRequest = jwtTokenProvider.resolveToken(request); // 예시: 로그아웃 시 리프레시 토큰도
																									// 같이 보내는 경우
					if (refreshTokenValueFromRequest != null) {
						refreshTokenService.deleteByTokenValue(refreshTokenValueFromRequest);
					}

				} catch (Exception e) {
					log.error("로그아웃 시 Refresh Token 삭제 실패: {}", e.getMessage());
				}
			}
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			Map<String, String> logoutResponse = new HashMap<>();
			logoutResponse.put("message", "로그아웃 되었습니다.");
			new ObjectMapper().writeValue(response.getWriter(), logoutResponse);
			// 또는 프론트엔드 홈으로 리다이렉트
			// response.sendRedirect("http://localhost:5173/");
		};
	}

	// CORS 설정을 위한 Bean
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://192.168.205.56:5173", "http://192.168.205.63:5173")); // Vue.js 개발 서버 주소		
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
		configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
		configuration.setAllowCredentials(true); // 쿠키를 포함한 요청 허용
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 적용
		return source;
	}

	// CustomOAuth2UserService를 빈으로 등록 (만약 @Service 어노테이션이 없다면)
	// CustomOAuth2UserService에 @Service가 있다면 이 빈 정의는 필요 없음
	// @Bean
	// public OAuth2UserService<OAuth2UserRequest, OAuth2User>
	// customOAuth2UserService(MemberService memberService) {
	// return new CustomOAuth2UserService(memberService); // CustomOAuth2UserService
	// 생성자에 맞게 수정
	// }
}