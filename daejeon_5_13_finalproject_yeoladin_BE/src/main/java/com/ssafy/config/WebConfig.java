package com.ssafy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Value("${spring.servlet.multipart.location}")
    String filePath;
	
	@Value("${file.upload-dir}")
    private String uploadDir;
	
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        
        registry.addResourceHandler("/videos/**") // 요청 URL
        .addResourceLocations("file:" + uploadDir + "/"); // 실제 경로
        
        registry.addResourceHandler("/images/**") // 요청 URL
        .addResourceLocations("file:"+filePath); // 실제 경로
        
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
}
