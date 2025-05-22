package com.mindmate.mindmate_server.auth.controller;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입, 로그인 등 인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @GetMapping("/oauth2/redirect")
    public ResponseEntity<TokenResponse> handleOAuth2Redirect(@RequestParam String token, @RequestParam String refreshToken) {
        TokenResponse response = TokenResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 세션을 종료하고 토큰을 무효화합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.replace("Bearer ", ""));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "토큰 갱신",
            description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Authorization") String refreshToken,
            @RequestHeader(value = "Access-Token", required = false) String accessToken) {
        TokenResponse response = authService.refresh(refreshToken.replace("Bearer ", ""),
                accessToken != null ? accessToken.replace("Bearer ", "") : null);
        return ResponseEntity.ok(response);
    }

}
