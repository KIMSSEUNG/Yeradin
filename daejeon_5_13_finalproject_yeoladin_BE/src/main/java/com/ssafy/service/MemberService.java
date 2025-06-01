package com.ssafy.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dto.MemberDto;
import com.ssafy.dto.OAuth2MemberDto;
import com.ssafy.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {
	private final MemberRepository memberRepo;
	private final PasswordEncoder passwordEncoder;

	// 일반 유저
	public int insertMember(MemberDto member) throws Exception {
		// 비밀번호 해싱
		member.setPw(passwordEncoder.encode(member.getPw()));
		// 일반 가입자는 provider를 "LOCAL"로 설정
		member.setProvider("LOCAL");
		// DTO에 role이 있다면 설정, 없다면 DB 기본값 'ROLE_USER' 사용 (Mapper에서 처리 가능)
		if (member.getRole() == null || member.getRole().isEmpty()) {
			member.setRole("ROLE_USER");
		}
		return memberRepo.insertMember(member);
	}

	public MemberDto selectMember(String id) throws Exception {
		return memberRepo.selectMember(id);
	}

	public int updateMember(MemberDto member) throws Exception {
		// 비밀번호 변경 요청이 있을 경우에만 해싱하여 업데이트
		if (member.getPw() != null && !member.getPw().isEmpty()) {
			member.setPw(passwordEncoder.encode(member.getPw()));
		} else {
			// 비밀번호 변경 요청이 없으면 기존 비밀번호 유지를 위해 pw 필드를 null로 설정 (Mapper에서 조건 처리)
			member.setPw(null);
		}
		return memberRepo.updateMember(member);
	}

	public int deleteMember(String id) throws Exception {

		int cnt = memberRepo.deleteMember(id);
		return cnt;
	}

	public MemberDto selectMemberByEmail(String email) throws Exception {
		return memberRepo.findByEmail(email); // 또는 기존 구현 방식
	}

	// form 로그인시
//	public boolean login(String id, String pw) throws Exception {
//		MemberDto dto = this.selectMember(id);
//		System.out.println(dto.toString());
//		if (dto != null && dto.getPw().equals(pw)) {
//			return true;
//		}
//		
//		return false;
//	}

	// OAuth2 유저
	@Transactional
	public int insertOAuth2Member(OAuth2MemberDto member) throws Exception {
		// System.out.println("Inserting OAuth2 Member: " + member.toString());
		return memberRepo.insertOAuth2Member(member); // DTO에 pk가 설정됨 (useGeneratedKeys)
	}

	// 이메일과 프로바이더로 OAuth 사용자 조회 (CustomOAuth2UserService에서 사용은 안하지만 필요할 수 있음)
	public Optional<OAuth2MemberDto> selectOAuth2MemberByEmailAndProvider(String email, String provider)
			throws Exception {
		return memberRepo.selectOAuth2MemberByEmailAndProvider(email, provider);
	}

	// provider_id와 provider로 OAuth 사용자 조회 (필요시 사용)
	public Optional<OAuth2MemberDto> selectOAuth2MemberByProviderId(String provider, String providerId)
			throws Exception {
		return memberRepo.selectOAuth2MemberByProviderId(provider, providerId);
	}

	@Transactional
	public int updateOAuth2Member(OAuth2MemberDto member) throws Exception {
		return memberRepo.updateOAuth2Member(member);
	}

	// 기존 사용자의 provider 정보 업데이트용 메소드 추가
	@Transactional
	public int updateOAuth2MemberProvider(OAuth2MemberDto member) throws Exception {
		return memberRepo.updateOAuth2MemberProvider(member);
	}

	@Transactional
	public int deleteOAuth2Member(String provider, String providerId) throws Exception { // provider, providerId로 삭제
		return memberRepo.deleteOAuth2Member(provider, providerId);
	}
}
