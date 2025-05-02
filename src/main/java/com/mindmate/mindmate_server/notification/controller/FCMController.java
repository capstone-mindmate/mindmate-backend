package com.mindmate.mindmate_server.notification.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.notification.dto.FCMTokenRequest;
import com.mindmate.mindmate_server.notification.dto.FCMTokenResponse;
import com.mindmate.mindmate_server.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/fcm/token")
@RequiredArgsConstructor
@Tag(name = "FCM", description = "FCM 토큰 관리 API")
public class FCMController {
    private final FCMService fcmService;

    @PostMapping
    @Operation(summary = "FCM 토큰 등록", description = "사용자의 FCM 토큰을 등록합니다.")
    public ResponseEntity<FCMTokenResponse> registerToken(
            @RequestBody FCMTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FCMTokenResponse fcmTokenResponse = fcmService.registerToken(principal.getUserId(), request);
        return ResponseEntity.ok(fcmTokenResponse);
    }

    @DeleteMapping
    @Operation(summary = "FCM 토큰 비활성화", description = "사용자의 FCM 토큰을 비활성화합니다.")
    public ResponseEntity<FCMTokenResponse> deactivateToken(
            @RequestBody FCMTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FCMTokenResponse fcmTokenResponse = fcmService.deactivateToken(principal.getUserId(), request);
        return ResponseEntity.ok(fcmTokenResponse);
    }
}