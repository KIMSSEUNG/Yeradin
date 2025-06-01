package com.ssafy.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.dto.MemberDto;
import com.ssafy.dto.OAuth2MemberDto;

@Mapper
public interface MemberRepository {

	public int insertMember(MemberDto member) throws Exception;

	public MemberDto selectMember(String email) throws Exception;

	public int updateMember(MemberDto member) throws Exception;

	public int deleteMember(String id) throws Exception;

	public int insertOAuth2Member(OAuth2MemberDto member) throws Exception; // useGeneratedKeys="true" keyProperty="pk"
																			// 사용

	MemberDto findByEmail(@Param("email") String email) throws Exception;
	
	public Optional<OAuth2MemberDto> selectOAuth2MemberByEmailAndProvider(@Param("email") String email,
			@Param("provider") String provider) throws Exception;

	// 프로바이더 ID로 조회 (provider도 함께)
	public Optional<OAuth2MemberDto> selectOAuth2MemberByProviderId(@Param("provider") String provider,
			@Param("provider_id") String provider_id) throws Exception;

	public int updateOAuth2Member(OAuth2MemberDto member) throws Exception;

	// provider 정보 업데이트용
	public int updateOAuth2MemberProvider(OAuth2MemberDto member) throws Exception;

	public int deleteOAuth2Member(@Param("provider") String provider, @Param("provider_id") String provider_id)
			throws Exception; // provider, providerId로 삭제
}
