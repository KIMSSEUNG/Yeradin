package com.ssafy.repository;

import com.ssafy.dto.RefreshTokenDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param; // 여러 파라미터 전달 시 필요

import java.util.Optional;

@Mapper // MyBatis Mapper임을 명시
public interface RefreshTokenRepository {

    // 토큰 값으로 조회
    Optional<RefreshTokenDto> findByTokenValue(@Param("tokenValue") String tokenValue);

    // memberId로 조회 (한 사용자당 하나의 토큰만 유지하는 경우)
    Optional<RefreshTokenDto> findByMemberId(@Param("memberId") Long memberId);

    // Refresh Token 저장 (INSERT)
    int save(RefreshTokenDto refreshToken);

    // Refresh Token 업데이트 (기존 토큰 갱신 시)
    int update(RefreshTokenDto refreshToken);

    // memberId로 삭제
    int deleteByMemberId(@Param("memberId") Long memberId);

    // tokenValue로 삭제
    int deleteByTokenValue(@Param("tokenValue") String tokenValue);

    // 만료된 모든 토큰 삭제 (배치 작업용, 선택 사항)
    // int deleteExpiredTokens();
}