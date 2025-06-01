package com.ssafy.security;

import com.ssafy.dto.MemberDto; // 일반 회원 DTO도 사용 가능성 있음
import com.ssafy.dto.OAuth2MemberDto;
import com.ssafy.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 추가

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private MemberService memberService;

    @Override
    @Transactional // 여러 DB 작업을 하나의 트랜잭션으로 묶기
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google", "kakao" 등
        String userNameAttributeName = userRequest.getClientRegistration()
                                                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // "sub" for google, "id" for kakao/naver

        Map<String, Object> attributes = oAuth2User.getAttributes();
        logger.info("OAuth2 User Attributes from {}: {}", registrationId, attributes);
        
        String id = "";
        String email = "";
        String name = "";
        String providerId = ""; // OAuth 공급자별 고유 ID

        // OAuth 공급자별로 속성 추출 방식이 다를 수 있음
        if ("google".equalsIgnoreCase(registrationId)) {
        	id = (String) attributes.get("id");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("sub"); // Google의 경우 sub가 고유 ID
        }

        if (email == null || email.isEmpty()) {
            logger.error("Email not found from OAuth2 provider: {}", registrationId);
            // 이메일이 없는 경우 providerId를 email 대신 사용할 수도 있으나, 일반적으로 이메일은 필수
            // email = providerId + "@" + registrationId + ".temp"; // 임시 이메일 생성 (정책에 따라 다름)
             throw new OAuth2AuthenticationException("Email not found from OAuth2 provider. Attributes: " + attributes);
        }

        // DB에서 email로 사용자 조회 (MemberDto로 조회 후 필요시 OAuth2MemberDto로 변환/처리)
        MemberDto existingMemberDto = null;
        try {
            existingMemberDto = memberService.selectMember(email); // 일반 selectMember 사용 (email 기준)
        } catch (Exception e) {
            logger.error("Error fetching user by email: " + email, e);
            throw new RuntimeException("Error fetching user information", e);
        }

        OAuth2MemberDto finalMemberDto; // Spring Security와 우리 앱에서 사용할 최종 사용자 DTO

        if (existingMemberDto != null) { // 이메일로 가입된 사용자가 존재
            logger.info("Existing user found by email: {}", email);
            // MemberDto를 OAuth2MemberDto 형태로 변환 (필요한 정보 채우기)
            finalMemberDto = new OAuth2MemberDto();
            finalMemberDto.setId(existingMemberDto.getId());
            finalMemberDto.setEmail(existingMemberDto.getEmail());
            finalMemberDto.setName(name); // OAuth에서 받은 이름으로 업데이트 할 수도 있음

            // 기존 사용자의 provider 정보 확인 및 업데이트
            if (!registrationId.equalsIgnoreCase(existingMemberDto.getProvider())) {
                logger.warn("User {} already exists with provider {}. Updating to provider {} and provider_id {}",
                        email, existingMemberDto.getProvider(), registrationId.toUpperCase(), providerId);
                finalMemberDto.setProvider(registrationId.toUpperCase());
                finalMemberDto.setProvider_id(providerId);
                // DB 업데이트 (provider, provider_id)
                try {
                    memberService.updateOAuth2MemberProvider(finalMemberDto); // 이 메서드 MemberService에 추가 필요
                } catch (Exception e) {
                    logger.error("Failed to update provider info for user: " + email, e);
                    throw new RuntimeException("Failed to update user provider info", e);
                }
            } else {
                // 이미 해당 OAuth로 가입된 사용자. 정보 최신화 (이름 등)
                finalMemberDto.setProvider(existingMemberDto.getProvider());
                finalMemberDto.setProvider_id(existingMemberDto.getProvider_id());
                 if(!name.equals(existingMemberDto.getName())){ // 이름이 변경된 경우
                    try {
                        memberService.updateOAuth2Member(finalMemberDto); // 이름 등 업데이트
                    } catch (Exception e) {
                         logger.error("Failed to update OAuth user info: " + email, e);
                    }
                }
            }
        } else {
            // 신규 회원인 경우 회원가입 처리
            logger.info("New user. Registering with email: {}, provider: {}, provider_id: {}", email, registrationId, providerId);
            finalMemberDto = new OAuth2MemberDto();
            finalMemberDto.setEmail(email);
            finalMemberDto.setName(name);
            finalMemberDto.setProvider(registrationId.toUpperCase());
            finalMemberDto.setProvider_id(providerId);
            // pw는 OAuth 사용자의 경우 불필요 (DB 스키마상 nullable)

            try {
                memberService.insertOAuth2Member(finalMemberDto); // 셔 🔑 이 호출 후 finalMemberDto.id에 값이 있어야 함
                logger.info("New user registered: {}, pk from DTO after insert: {}", email, finalMemberDto.getId()); // 셔 👈 이 로그 반드시 확인!
                if (finalMemberDto.getId() == null) { // 셔 🔑 getId()가 null이면 여기가 문제!
                    // 이 블록은 getId()가 null일 때의 예비 처리인데, 근본적으로 insertOAuth2Member에서 ID를 받아와야 합니다.
                    logger.warn("finalMemberDto.getId() is null after insertOAuth2Member. Attempting to re-fetch...");
                    MemberDto newlyInserted = memberService.selectMember(email); // 이메일로 다시 조회
                    if (newlyInserted != null && newlyInserted.getId() != null) {
                        finalMemberDto.setId(newlyInserted.getId());
                        logger.info("Successfully re-fetched user PK: {}", finalMemberDto.getId());
                    } else {
                         logger.error("Failed to retrieve pk for newly inserted user even after re-fetch: {}", email);
                         // 이 경우 SecurityConfig의 authenticationSuccessHandler에서 memberPk가 null이 되어 프론트에 id가 전달되지 않음
                         throw new RuntimeException("Failed to retrieve pk after user registration and re-fetch");
                    }
                }
            } catch (Exception e) {
                logger.error("OAuth2 user registration failed for email: " + email, e);
                throw new RuntimeException("OAuth2 user registration failed", e);
            }
        }
        
        // Spring Security가 사용할 OAuth2User 객체 생성
        // attributes에 우리 시스템의 사용자 정보 (예: pk, DB에서 가져온 이름)를 추가하여 successHandler에서 활용 가능
        Map<String, Object> customAttributes = new java.util.HashMap<>(attributes); // 원본 attributes 복사
        customAttributes.put("userPk", finalMemberDto.getId());
        customAttributes.put("email", finalMemberDto.getEmail()); // 정제된 이메일
        customAttributes.put("name", finalMemberDto.getName());   // DB 또는 OAuth에서 가져온 이름
        customAttributes.put("provider", finalMemberDto.getProvider());
        
        logger.info("Final member DTO before putting to customAttributes - ID: {}", finalMemberDto.getId());
        customAttributes.put("userPk", finalMemberDto.getId()); // 여기서 finalMemberDto.getId()가 null이면 끝
        // userNameAttributeName는 OAuth 공급자가 사용자를 식별하는 기본 키 (예: 'sub', 'id')
        // DefaultOAuth2User는 이 키를 사용하여 attributes에서 주 사용자 이름을 찾으려고 함.
        // 우리 시스템에서는 email이나 pk를 주로 사용하므로, userNameAttributeName에 너무 의존하지 않아도 됨.
        // 다만, Spring Security 내부 로직을 위해 올바른 값을 전달하는 것이 좋음.
        // 만약 customAttributes에 userNameAttributeName에 해당하는 키가 없다면 에러 발생 가능성 있음
        // => attributes 원본을 사용하거나, customAttributes에 해당 key-value를 포함시켜야 함.
        // 여기서는 nameAttributeKey로 email을 사용하도록 지정하고, customAttributes에 email을 넣어줌.
        String nameAttributeKey = "email"; // 우리 시스템에서 주요 식별자로 사용할 속성 키 (successHandler 등에서 사용)

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                customAttributes, // 우리 앱의 정보가 포함된 attributes
                nameAttributeKey // Principal의 이름을 어떤 attribute에서 가져올지 지정
        );
    }
}