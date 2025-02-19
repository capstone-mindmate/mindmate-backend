package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.dto.*;
import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.auth.util.PasswordValidator;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static com.mindmate.mindmate_server.auth.service.LoginAttemptService.MAX_ATTEMPTS;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final PasswordValidator passwordValidator;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(SignUpRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        passwordValidator.validatePassword(request.getPassword());
        passwordValidator.validatePasswordMatch(request.getPassword(), request.getConfirmPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RoleType.ROLE_UNVERIFIED)
                .build();

        user.generateVerificationToken();

        userService.save(user);
        emailService.sendVerificationEmail(user, user.getVerificationToken(), "init");
    }

//    public void resendVerificationEmail(String email) {
//        User user = userService.findByEmail(email);
//
//        if (user.isEmailVerified()) {
//            throw new CustomException(AuthErrorCode.EMAIL_ALREADY_VERIFIED);
//        }
//
//        user.generateVerificationToken();
//        userService.save(user);
//        emailService.sendVerificationEmail(user, user.getVerificationToken(), "resend");
//    }

    @Override
    public void verifyEmail(String token) {
        User user = userService.findVerificationToken(token);

        if (user.isTokenExpired()) {
            throw new CustomException(AuthErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        user.verifyEmail();
        user.updateRole(RoleType.ROLE_USER);
        userService.save(user);
    }

    /**
     * 로그인
     * 1. 유효성 검사 (로그인 시도 횟수, 사용자, 비밀번호)
     * 2. 로그인 실패/성공 여부에 따른 처리
     * 3. TokenFamily - AccessToken - RefreshToken 저장
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();

        if (loginAttemptService.isBlocked(email)) {
            Long remainingMinutes = loginAttemptService.getRemainingLockTime(email);
            throw new CustomException(AuthErrorCode.ACCOUNT_LOCKED, Map.of("remainingTime", remainingMinutes));
        }

        User user = userService.findByEmail(email);
        if (!user.isEmailVerified()) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(email);
            int remainingAttempts = MAX_ATTEMPTS - loginAttemptService.getCurrentAttempts(email);

            throw new CustomException(
                    remainingAttempts > 0
                            ? AuthErrorCode.REMAINING_ATTEMPTS
                            : AuthErrorCode.ACCOUNT_LOCKED,
                    Map.of("remainingAttempts", remainingAttempts));
        }

        loginAttemptService.loginSucceeded(email);
        String tokenFamily = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, tokenFamily);

        tokenService.saveRefreshToken(user.getId(), refreshToken, tokenFamily);

        boolean hasListenerProfile = user.getListenerProfile() != null;
        boolean hasSpeakerProfile = user.getSpeakerProfile() != null;
        String message = user.getCurrentRole() == RoleType.ROLE_USER
                ? "프로필 작성이 필요합니다."
                : null;

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .currentRole(user.getCurrentRole())
                .hasListenerProfile(hasListenerProfile)
                .hasSpeakerProfile(hasSpeakerProfile)
                .message(message)
                .build();
    }

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
