package com.mindmate.mindmate_server.notification.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.notification.dto.FCMTokenRequest;
import com.mindmate.mindmate_server.notification.dto.FCMTokenResponse;
import com.mindmate.mindmate_server.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm/token")
@RequiredArgsConstructor
public class FCMController {
    private final FCMService fcmService;

    @PostMapping
    public ResponseEntity<FCMTokenResponse> registerToken(
            @RequestBody FCMTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        fcmService.registerToken(principal.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<FCMTokenResponse> deactivateToken(
            @RequestParam FCMTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        fcmService.deactivateToken(principal.getUserId(), request);
        return ResponseEntity.ok().build();
    }
}