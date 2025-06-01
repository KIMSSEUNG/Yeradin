package com.ssafy.security; // 또는 적절한 패키지

import com.ssafy.dto.MemberDto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final MemberDto memberDto;

    public CustomUserDetails(MemberDto memberDto) {
        this.memberDto = memberDto;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // DB의 role 필드를 기반으로 권한 설정 (예: "ROLE_USER")
        return Collections.singletonList(new SimpleGrantedAuthority(memberDto.getRole() != null ? memberDto.getRole() : "ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return memberDto.getPw(); // DB에 저장된 해시된 비밀번호
    }

    @Override
    public String getUsername() {
        return memberDto.getEmail(); // 로그인 ID로 email 사용
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화 여부
    }

    public MemberDto getMemberDto() { // Controller 등에서 MemberDto 객체가 필요할 때 사용
        return memberDto;
    }
}