package com.mindmate.mindmate_server.auth.controller;

import com.mindmate.mindmate_server.auth.dto.LoginRequest;
import com.mindmate.mindmate_server.auth.dto.LoginResponse;
import com.mindmate.mindmate_server.auth.dto.SignUpRequest;
import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입, 로그인, 이메일 인증 등 인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록하고 인증 메일을 발송합니다.")
    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@Valid @RequestBody SignUpRequest request) {
        authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "인증 메일 재발송", description = "이메일 인증 메일을 재발송합니다.")
    @PostMapping("/email/resend")
    public ResponseEntity<Void> resendVerificationEmail(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이메일 인증", description = "이메일 인증 토큰을 검증합니다.")
    @GetMapping("/email/verify")
    public ResponseEntity<TokenResponse> verifyEmail(@RequestParam String token) {
        TokenResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.replace("Bearer ", ""));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Authorization") String refreshToken,
            @RequestHeader(value = "Access-Token", required = false) String accessToken) {
        TokenResponse response = authService.refresh(refreshToken.replace("Bearer ", ""),
                accessToken != null ? accessToken.replace("Bearer ", "") : null);
        return ResponseEntity.ok(response);
    }

}
