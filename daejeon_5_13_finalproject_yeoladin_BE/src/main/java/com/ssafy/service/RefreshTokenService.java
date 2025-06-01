package com.ssafy.service;

import com.ssafy.dto.RefreshTokenDto;
import com.ssafy.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Spring의 트랜잭션 사용

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository; // MyBatis Mapper 주입

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshTokenValiditySeconds;

    @Transactional // 데이터 변경이 있으므로 트랜잭션 처리
    public RefreshTokenDto saveOrUpdateRefreshToken(Long memberId, String tokenValue) {
        Optional<RefreshTokenDto> existingTokenOpt = refreshTokenRepository.findByMemberId(memberId);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds);
        LocalDateTime issuedAt = LocalDateTime.now();

        RefreshTokenDto refreshToken;
        if (existingTokenOpt.isPresent()) {
            refreshToken = existingTokenOpt.get();
            refreshToken.setTokenValue(tokenValue); // DTO의 setter 사용
            refreshToken.setIssuedAt(issuedAt);
            refreshToken.setExpiresAt(expiresAt);
            refreshTokenRepository.update(refreshToken); // update 쿼리 호출
        } else {
            refreshToken = RefreshTokenDto.builder()
                    .memberId(memberId)
                    .tokenValue(tokenValue)
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .build();
            refreshTokenRepository.save(refreshToken); // save (insert) 쿼리 호출
        }
        // save/update 후 id가 채워진 refreshToken 객체를 반환하거나, memberId로 다시 조회하여 반환할 수 있음.
        // MyBatis의 useGeneratedKeys="true" keyProperty="id" 설정으로 save 후 id가 DTO에 채워짐.
        return refreshToken;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshTokenDto> findByTokenValue(String tokenValue) {
        Optional<RefreshTokenDto> refreshTokenOpt = refreshTokenRepository.findByTokenValue(tokenValue);
        if (refreshTokenOpt.isPresent() && refreshTokenOpt.get().isExpired()) {
            // 만료된 토큰이면 삭제하고 빈 Optional 반환
            refreshTokenRepository.deleteByTokenValue(refreshTokenOpt.get().getTokenValue());
            return Optional.empty();
        }
        return refreshTokenOpt;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshTokenDto> findByMemberId(Long memberId) {
        return refreshTokenRepository.findByMemberId(memberId);
    }

    @Transactional
    public void deleteByMemberId(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    @Transactional
    public void deleteByTokenValue(String tokenValue) {
        refreshTokenRepository.deleteByTokenValue(tokenValue);
    }
}