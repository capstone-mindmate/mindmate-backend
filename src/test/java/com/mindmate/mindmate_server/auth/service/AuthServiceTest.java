package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.dto.*;
import com.mindmate.mindmate_server.auth.service.AuthServiceImpl;
import com.mindmate.mindmate_server.auth.service.EmailService;
import com.mindmate.mindmate_server.auth.service.LoginAttemptService;
import com.mindmate.mindmate_server.auth.service.TokenService;
import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.auth.util.PasswordValidator;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;

import static com.mindmate.mindmate_server.auth.service.AuthServiceImpl.RESEND_LIMIT_MINUTES;
import static com.mindmate.mindmate_server.auth.service.LoginAttemptService.MAX_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserService userService;
    @Mock private TokenService tokenService;
    @Mock private EmailService emailService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private PasswordValidator passwordValidator;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class RegisterUserTest {

        @Test
        @DisplayName("정상적인 회원가입 성공")
        void registerUser_Success() {
            // given
            SignUpRequest request = new SignUpRequest("test@email.com", "password123", "password123");
            when(userService.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

            // when
            authService.registerUser(request);

            // then
            verify(userService).existsByEmail(request.getEmail());
            verify(passwordValidator).validatePassword(request.getPassword());
            verify(userService).save(any(User.class));
            verify(emailService).sendVerificationEmail(any(User.class), anyString());
        }

        @Test
        @DisplayName("중복 이메일")
        void registerUser_DuplicateEmail() {
            // given
            SignUpRequest request = new SignUpRequest("test@email.com", "password123", "password123");
            when(userService.existsByEmail(anyString())).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> authService.registerUser(request));
            verify(userService).existsByEmail(request.getEmail());
        }

        @Test
        @DisplayName("비밀번호 검증 실패")
        void registerUser_PasswordMismatch() {
            // given
            SignUpRequest request = new SignUpRequest("test@email.com", "password123", "password456");
            doThrow(new CustomException(AuthErrorCode.PASSWORD_MISMATCH))
                    .when(passwordValidator)
                    .validatePasswordMatch(request.getPassword(), request.getConfirmPassword());

            // when & then
            assertThrows(CustomException.class, () -> authService.registerUser(request));
        }
    }

    @Nested
    @DisplayName("이메일 재전송")
    class ResendVerificationEmail {
        @Test
        @DisplayName("정상 재전송")
        void resendVerificationEmail_Success() {
            // given
            String email = "test@email.com";
            String verificationToken = "test-token";
            User user = mock(User.class);

            when(userService.findByEmail(email)).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(false);
            when(user.getVerificationToken()).thenReturn(verificationToken);
            when(valueOperations.get(anyString())).thenReturn(null);

            // when
            authService.resendVerificationEmail(email);

            // then
            verify(user).getVerificationToken();
            verify(userService).save(user);
            verify(emailService).sendVerificationEmail(eq(user), eq(verificationToken));
        }

        @Test
        @DisplayName("이미 인증된 이메일")
        void resendVerificationEmail_AlreadyVerified() {
            // given
            String email = "test@email.com";
            User user = mock(User.class);

            when(userService.findByEmail(email)).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> authService.resendVerificationEmail(email));
        }

        @Test
        @DisplayName("재전송 제한 시간")
        void resendVerificationEmail_TooFrequent() {
            // given
            String email = "test@email.com";
            User user = mock(User.class);

            when(userService.findByEmail(email)).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(false);
            when(valueOperations.get(anyString()))
                    .thenReturn(LocalDateTime.now().toString());

            // when & then
            assertThrows(CustomException.class, () -> authService.resendVerificationEmail(email));
        }
    }

    @Nested
    @DisplayName("이메일 인증 테스트")
    class VerifyEmailTest {
        @Test
        @DisplayName("정상적인 이메일 인증 성공")
        void verifyEmail_Success() {
            // given
            String token = "valid-token";
            User user = mock(User.class);

            when(userService.findVerificationToken(token)).thenReturn(user);
            when(user.getVerificationToken()).thenReturn(token);
            when(user.isTokenExpired()).thenReturn(false);

            // when
            authService.verifyEmail(token);

            // then
            verify(userService).findVerificationToken(token);
            verify(user).verifyEmail();
            verify(user).updateRole(RoleType.ROLE_USER);
            verify(userService).save(user);
        }

        @Test
        @DisplayName("잘못된 토큰으로 인증 시도")
        void verifyEmail_InvalidToken() {
            // given
            String token = "invalid-token";
            when(userService.findVerificationToken(token)).thenReturn(null);

            // when & then
            assertThrows(CustomException.class, () -> authService.verifyEmail(token));
        }

        @Test
        @DisplayName("만료된 토큰으로 인증 시도")
        void verifyEmail_ExpiredToken() {
            // given
            String token = "expired-token";
            User user = mock(User.class);

            when(userService.findVerificationToken(token)).thenReturn(user);
            when(user.getVerificationToken()).thenReturn(token);
            when(user.isTokenExpired()).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> authService.verifyEmail(token));
        }

        @Test
        @DisplayName("재전송 제한 시간 초과하지 않은 경우")
        void resendVerificationEmail_TooFrequent() {
            // given
            String email = "test@email.com";
            User user = mock(User.class);

            LocalDateTime recentRequestTime = LocalDateTime.now().minusMinutes(RESEND_LIMIT_MINUTES - 1);
            when(userService.findByEmail(email)).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(false);

            when(valueOperations.get(anyString())).thenReturn(recentRequestTime.toString());

            // when & then
            assertThrows(CustomException.class, () -> authService.resendVerificationEmail(email));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {
        @Test
        @DisplayName("정상 로그인")
        void login_Success() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "password123");
            User user = mock(User.class);

            when(loginAttemptService.isBlocked(request.getEmail())).thenReturn(false);
            when(userService.findByEmail(request.getEmail())).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(true);
            when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            when(jwtTokenProvider.generateToken(any(User.class))).thenReturn(accessToken);
            when(jwtTokenProvider.generateRefreshToken(any(User.class), anyString())).thenReturn(refreshToken);

            // when
            LoginResponse response = authService.login(request);

            // then
            assertNotNull(response);
            assertEquals(accessToken, response.getAccessToken());
            assertEquals(refreshToken, response.getRefreshToken());

            verify(loginAttemptService).loginSucceeded(request.getEmail());
        }

        @Test
        @DisplayName("계정 잠금 상태에서 로그인 시도")
        void login_AccountLocked() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "password123");
            when(loginAttemptService.isBlocked(request.getEmail())).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("이메일 미인증 상태에서 로그인 시도")
        void login_EmailNotVerified() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "password123");
            User user = mock(User.class);

            when(loginAttemptService.isBlocked(request.getEmail())).thenReturn(false);
            when(userService.findByEmail(request.getEmail())).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(false);

            // when & then
            assertThrows(CustomException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("비밀번호 불일치 로그인 실패")
        void login_PasswordMismatch() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "wrong-password");
            User user = mock(User.class);

            when(loginAttemptService.isBlocked(request.getEmail())).thenReturn(false);
            when(userService.findByEmail(request.getEmail())).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(true);
            when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

            // when & then
            assertThrows(CustomException.class, () -> authService.login(request));
            verify(loginAttemptService).loginFailed(request.getEmail());
        }

        @Test
        @DisplayName("프로필 작성이 필요한 경우 메시지 반환")
        void login_ProfileMessageNeeded() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "password123");
            User user = mock(User.class);

            when(loginAttemptService.isBlocked(request.getEmail())).thenReturn(false);
            when(userService.findByEmail(request.getEmail())).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(true);
            when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
            when(user.getCurrentRole()).thenReturn(RoleType.ROLE_USER);

            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            when(jwtTokenProvider.generateToken(user)).thenReturn(accessToken);
            when(jwtTokenProvider.generateRefreshToken(eq(user), anyString())).thenReturn(refreshToken);

            // when
            LoginResponse response = authService.login(request);

            // then
            assertNotNull(response);
            assertEquals("프로필 작성이 필요합니다.", response.getMessage());
        }

        @Test
        @DisplayName("로그인 시도가 횟수 남지 않아 계정이 잠긴 경우")
        void login_AccountLockedAfterMaxAttempts() {
            // given
            LoginRequest request = new LoginRequest("test@email.com", "wrong-password");
            User user = mock(User.class);

            when(loginAttemptService.isBlocked(request.getEmail())).thenReturn(false);
            when(userService.findByEmail(request.getEmail())).thenReturn(user);
            when(user.isEmailVerified()).thenReturn(true);
            when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);
            when(loginAttemptService.getCurrentAttempts(request.getEmail())).thenReturn(MAX_ATTEMPTS);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> authService.login(request));

            assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());;
            Map<String, Object> details = (Map<String, Object>) exception.getDetails();
            assertNotNull(details);
            assertEquals(0, details.get("remainingAttempts"));

            verify(loginAttemptService).loginFailed(request.getEmail());
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {
        @Test
        @DisplayName("정상적인 로그아웃 성공")
        void logout_Success() {
            // given
            String token = "valid-access-token";
            Long userId = 1L;

            when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);

            // when
            authService.logout(token);

            // then
            verify(tokenService).addToBlackList(token);
            verify(tokenService).invalidateRefreshToken(userId);
        }
    }

    @Nested
    @DisplayName("토큰 갱신 테스트")
    class RefreshTest {
        @Test
        @DisplayName("정상적인 토큰 갱신 성공")
        void refresh_Success() {
            // given
            String refreshToken = "valid-refresh-token";
            String accessToken = "old-access-token";

            Long userId = 1L;
            User user = mock(User.class);
            TokenData tokenData = TokenData.of(refreshToken, "token-family");

            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
            when(jwtTokenProvider.getTokenTypeFromToken(refreshToken)).thenReturn("REFRESH");
            when(userService.findUserById(userId)).thenReturn(user);
            when(jwtTokenProvider.getTokenFamilyFromToken(refreshToken)).thenReturn("token-family");
            when(tokenService.getRefreshToken(userId)).thenReturn(tokenData);

            when(jwtTokenProvider.generateToken(user)).thenReturn(newAccessToken);
            when(jwtTokenProvider.generateRefreshToken(eq(user), anyString())).thenReturn(newRefreshToken);

            // when
            TokenResponse response = authService.refresh(refreshToken, accessToken);

            // then
            assertNotNull(response);
            assertEquals(newAccessToken, response.getAccessToken());
            assertEquals(newRefreshToken, response.getRefreshToken());

            verify(tokenService).addToBlackList(refreshToken);
            verify(tokenService).saveRefreshToken(eq(userId), eq(newRefreshToken), anyString());
        }

        @Test
        @DisplayName("리프레시 토큰 재사용 감지")
        void refresh_TokenReuseDetected() {
            // given
            String refreshToken = "valid-refresh-token";
            Long userId = 1L;

//            TokenData tokenData = null;

            setupRefreshMocks(refreshToken, userId, null);

            // when & then
            assertThrows(CustomException.class, () -> authService.refresh(refreshToken, null));
            verify(tokenService).invalidateRefreshToken(userId);
        }

        @Test
        @DisplayName("저장된 토큰 데이터가 null인 경우 예외 발생")
        void refresh_StoredTokenDataNull() {
            // given
            String refreshToken = "valid-refresh-token";
            Long userId = 1L;

            setupRefreshMocks(refreshToken, userId, null);

            // when & then
            assertThrows(CustomException.class, () -> authService.refresh(refreshToken, null));
        }

        @Test
        @DisplayName("토큰 패밀리가 일치하지 않는 경우 예외 발생")
        void refresh_TokenFamilyMismatch() {
            // given
            String refreshToken = "valid-refresh-token";
            Long userId = 1L;
            TokenData tokenData = TokenData.of("refresh-token", "different-family");

            setupRefreshMocks(refreshToken, userId, tokenData);

            // when & then
            assertThrows(CustomException.class, () -> authService.refresh(refreshToken, null));
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰으로 예외 발생")
        void refresh_InvalidRefreshToken() {
            // given
            String refreshToken = "invalid-refresh-token";
            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(false);

            // when & then
            assertThrows(CustomException.class, () -> authService.refresh(refreshToken, null));
        }

        @Test
        @DisplayName("리프레시 토큰 타입이 올바르지 않은 경우")
        void refresh_InvalidTokenType() {
            // given
            String refreshToken = "valid-refresh-token";
            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getTokenTypeFromToken(refreshToken)).thenReturn("ACCESS");

            // when & then
            assertThrows(CustomException.class, () -> authService.refresh(refreshToken, null));
        }

        @Test
        @DisplayName("유효한 액세스 토큰이 블랙리스트에 추가되는 경우")
        void refresh_AccessTokenBlacklisted() {
            // given
            String refreshToken = "valid-refresh-token";
            String accessToken = "valid-access-token";
            Long userId = 1L;
            User user = mock(User.class);

            TokenData tokenData = TokenData.of(refreshToken, "token-family");

            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.validateToken(accessToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
            when(userService.findUserById(userId)).thenReturn(user);

            setupRefreshMocks(refreshToken, userId, tokenData);

            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            when(jwtTokenProvider.generateRefreshToken(eq(user), anyString())).thenReturn(newRefreshToken);
            when(jwtTokenProvider.generateToken(eq(user))).thenReturn(newAccessToken);

            // when
            TokenResponse response = authService.refresh(refreshToken, accessToken);

            // then
            verify(tokenService).addToBlackList(accessToken);
        }

    }

    private void setupRefreshMocks(String refreshToken, Long userId, TokenData tokenData) {
        when(jwtTokenProvider.getTokenTypeFromToken(refreshToken)).thenReturn("REFRESH");
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(jwtTokenProvider.getTokenFamilyFromToken(refreshToken)).thenReturn("token-family");
        when(tokenService.getRefreshToken(userId)).thenReturn(tokenData);
    }

}
