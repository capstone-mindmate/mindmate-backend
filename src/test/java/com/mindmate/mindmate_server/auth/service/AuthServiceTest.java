package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.dto.TokenData;
import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock private UserService userService;
    @Mock private TokenService tokenService;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String REFRESH_TOKEN = "valid.refresh.token";
    private static final String ACCESS_TOKEN = "valid.access.token";
    private static final String TOKEN_FAMILY = "token-family-123";
    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final RoleType USER_ROLE = RoleType.ROLE_USER;

    private User user;
    private TokenData storedTokenData;

    @BeforeEach
    void setUp() {
        user = createUser(USER_ID, USER_EMAIL, USER_ROLE);
        storedTokenData = createTokenData(REFRESH_TOKEN, TOKEN_FAMILY);
    }

    private User createUser(Long id, String email, RoleType role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.getCurrentRole()).thenReturn(role);
        return user;
    }

    private TokenData createTokenData(String token, String tokenFamily) {
        TokenData tokenData = mock(TokenData.class);
        when(tokenData.getRefreshToken()).thenReturn(token);
        when(tokenData.getTokenFamily()).thenReturn(tokenFamily);
        return tokenData;
    }

    @Test
    @DisplayName("로그아웃 시 토큰 블랙리스트 처리 및 리프레시 토큰 삭제")
    void logout_ShouldAddToBlacklistAndInvalidateRefreshToken() {
        // given
        when(jwtTokenProvider.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);

        // when
        authService.logout(ACCESS_TOKEN);

        // then
        verify(tokenService).addToBlackList(ACCESS_TOKEN);
        verify(jwtTokenProvider).getUserIdFromToken(ACCESS_TOKEN);
        verify(tokenService).invalidateRefreshToken(USER_ID);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 새로운 토큰 발급")
    void refresh_ShouldGenerateNewTokens_WhenValidRefreshToken() {
        // given
        setupRefreshMocks(REFRESH_TOKEN, USER_ID, storedTokenData, TOKEN_FAMILY);
        when(jwtTokenProvider.validateToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("new.access.token");
        when(jwtTokenProvider.generateRefreshToken(eq(user), any(String.class))).thenReturn("new.refresh.token");

        // when
        TokenResponse result = authService.refresh(REFRESH_TOKEN, ACCESS_TOKEN);

        // then
        assertThat(result.getAccessToken()).isEqualTo("new.access.token");
        assertThat(result.getRefreshToken()).isEqualTo("new.refresh.token");
        verify(tokenService).addToBlackList(ACCESS_TOKEN);
        verify(tokenService).addToBlackList(REFRESH_TOKEN);
        verify(tokenService).saveRefreshToken(eq(USER_ID), eq("new.refresh.token"), any(String.class));
    }

    @ParameterizedTest
    @DisplayName("리프레시 토큰 검증 실패 시나리오")
    @MethodSource("invalidRefreshTokenScenarios")
    void refresh_ShouldThrowException_WhenInvalidRefreshToken(String scenario, String refreshToken,
                                                              boolean isValidToken, String tokenType, AuthErrorCode expectedError) {
        // given
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(isValidToken);
        if (isValidToken) {
            when(jwtTokenProvider.getTokenTypeFromToken(refreshToken)).thenReturn(tokenType);
        }

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.refresh(refreshToken, null));
        assertThat(exception.getErrorCode()).isEqualTo(expectedError);
    }

    static Stream<Arguments> invalidRefreshTokenScenarios() {
        return Stream.of(
                Arguments.of("유효하지 않은 토큰", "invalid.token", false, null, AuthErrorCode.INVALID_REFRESH_TOKEN),
                Arguments.of("잘못된 토큰 타입", "valid.token", true, "ACCESS", AuthErrorCode.INVALID_TOKEN_TYPE)
        );
    }

    @Test
    @DisplayName("토큰 재사용 감지 시 예외 발생")
    void refresh_ShouldThrowException_WhenTokenReuseDetected() {
        // given
        String differentTokenFamily = "different-token-family";
        TokenData storedTokenDataWithDifferentFamily = createTokenData(REFRESH_TOKEN, differentTokenFamily);
        setupRefreshMocks(REFRESH_TOKEN, USER_ID, storedTokenDataWithDifferentFamily, TOKEN_FAMILY);

        // when & then
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.refresh(REFRESH_TOKEN, null));
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.TOKEN_REUSE_DETECTED);
        verify(tokenService).invalidateRefreshToken(USER_ID);
    }

    @Test
    @DisplayName("토큰 패밀리가 일치하지 않는 경우 예외 발생")
    void refresh_TokenFamilyMismatch() {
        // given
        String differentTokenFamily = "different-token-family";
        TokenData tokenData = createTokenData(REFRESH_TOKEN, differentTokenFamily);
        setupRefreshMocks(REFRESH_TOKEN, USER_ID, tokenData, TOKEN_FAMILY);

        // when & then
        assertThrows(CustomException.class, () -> authService.refresh(REFRESH_TOKEN, null));
        verify(tokenService).invalidateRefreshToken(USER_ID);
    }

    private void setupRefreshMocks(String refreshToken, Long userId, TokenData tokenData, String tokenFamily) {
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getTokenTypeFromToken(refreshToken)).thenReturn("REFRESH");
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(userService.findUserById(userId)).thenReturn(user);
        when(jwtTokenProvider.getTokenFamilyFromToken(refreshToken)).thenReturn(tokenFamily);
        when(tokenService.getRefreshToken(userId)).thenReturn(tokenData);
    }
}