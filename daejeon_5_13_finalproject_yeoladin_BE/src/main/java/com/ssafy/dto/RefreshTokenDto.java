package com.ssafy.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter도 필요할 수 있음 (MyBatis 결과 매핑 등)

import java.time.LocalDateTime;

@Getter
@Setter // MyBatis가 결과를 매핑하거나, 서비스에서 값을 설정할 때 필요할 수 있습니다.
@NoArgsConstructor
public class RefreshTokenDto { // 클래스명을 Dto로 변경하여 구분 (선택 사항)

    private Long id; // PK
    private Long memberId; // Member 테이블의 ID
    private String tokenValue; // Refresh Token 값
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt; // Refresh Token 만료 시간

    @Builder
    public RefreshTokenDto(Long id, Long memberId, String tokenValue, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.memberId = memberId;
        this.tokenValue = tokenValue;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    // 필요에 따라 updateTokenValue 메소드 유지 또는 서비스 계층에서 처리
    public RefreshTokenDto updateToken(String newTokenValue, LocalDateTime newExpiresAt) {
        this.tokenValue = newTokenValue;
        this.expiresAt = newExpiresAt;
        this.issuedAt = LocalDateTime.now();
        return this;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}