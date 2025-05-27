package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.dto.TokenData;
import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그아웃
     * 액세스 토큰 블랙리스트 처리
     * 리프레시 토큰 삭제
     */
    @Override
    public void logout(String token) {
        tokenService.addToBlackList(token);

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        tokenService.invalidateRefreshToken(userId);
    }

    /**
     * 리프레시 토큰 관리 -> 액세스 토큰 만료 시
     * 1. 리프레시 토큰 검증
     * 2. 리프레시 토큰 재사용 방지
     * 3. 새로운 토큰 생성
     */
    @Override
    public TokenResponse refresh(String refreshToken, String accessToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN_TYPE);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userService.findUserById(userId);
        String tokenFamily = jwtTokenProvider.getTokenFamilyFromToken(refreshToken);
        TokenData storedTokenData = tokenService.getRefreshToken(userId);

        if (storedTokenData == null || !storedTokenData.getTokenFamily().equals(tokenFamily)) {
            tokenService.invalidateRefreshToken(userId);
            throw new CustomException(AuthErrorCode.TOKEN_REUSE_DETECTED);
        }

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            tokenService.addToBlackList(accessToken);
        }

        tokenService.addToBlackList(refreshToken);

        String newTokenFamily = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.generateToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user, newTokenFamily);

        tokenService.saveRefreshToken(userId, newRefreshToken, newTokenFamily);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
