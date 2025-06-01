package com.ssafy.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 허용
                .allowedOrigins("http://localhost:5173", "http://192.168.205.63:5173/", "http://192.168.205.56:5173/") // Vue 앱이 실행되는 Origin (필요시 추가 가능)
                // .allowedOrigins("*") // 모든 Origin을 허용하려면 이렇게 설정 (보안상 좋지 않음, 개발 단계에서만 사용)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 자격 증명(쿠키, HTTP 인증)을 허용
                .maxAge(3600); // Pre-flight 요청 결과를 1시간 캐시
    }
}