package com.mindmate.mindmate_server.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class TokenData {
    private final String refreshToken;
    private final String tokenFamily;
    private final LocalDateTime issuedAt;

    public static TokenData of(String refreshToken, String tokenFamily) {
        return TokenData.builder()
                .refreshToken(refreshToken)
                .tokenFamily(tokenFamily)
                .issuedAt(LocalDateTime.now())
                .build();
    }
}
