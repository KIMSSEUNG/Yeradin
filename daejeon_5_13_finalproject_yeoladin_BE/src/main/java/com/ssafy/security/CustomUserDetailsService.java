package com.ssafy.security; // 또는 적절한 패키지

import com.ssafy.dto.MemberDto;
import com.ssafy.service.MemberService; // MemberRepository를 직접 사용해도 무방
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private MemberService memberService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        MemberDto memberDto;
        try {
            memberDto = memberService.selectMember(email);
        } catch (Exception e) {
            // 실제 운영에서는 로깅을 추가하는 것이 좋습니다.
            throw new UsernameNotFoundException("DB 조회 중 오류 발생: " + email, e);
        }

        if (memberDto == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
        }

        // 일반 로컬 사용자(provider='LOCAL'이고 비밀번호가 있는 경우)만 이 서비스를 통해 인증
        if (!"LOCAL".equals(memberDto.getProvider()) || memberDto.getPw() == null || memberDto.getPw().isEmpty()) {
            throw new UsernameNotFoundException("일반 로그인 계정이 아니거나 비밀번호가 설정되지 않았습니다: " + email);
        }

        return new CustomUserDetails(memberDto);
    }
}