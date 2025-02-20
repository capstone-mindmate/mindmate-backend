package com.mindmate.mindmate_server.auth.controller;

import com.mindmate.mindmate_server.auth.dto.LoginRequest;
import com.mindmate.mindmate_server.auth.dto.LoginResponse;
import com.mindmate.mindmate_server.auth.dto.SignUpRequest;
import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@Valid @RequestBody SignUpRequest request) {
        authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/email/resend")
    public ResponseEntity<Void> resendVerificationEmail(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.replace("Bearer ", ""));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Authorization") String refreshToken,
            @RequestHeader(value = "Access-Token", required = false) String accessToken) {
        TokenResponse response = authService.refresh(refreshToken.replace("Bearer ", ""),
                accessToken != null ? accessToken.replace("Bearer ", "") : null);
        return ResponseEntity.ok(response);
    }

}
