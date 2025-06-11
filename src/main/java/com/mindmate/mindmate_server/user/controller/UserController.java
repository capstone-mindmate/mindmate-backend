package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.user.dto.PushNotificationSettingRequest;
import com.mindmate.mindmate_server.user.dto.PushNotificationSettingResponse;
import com.mindmate.mindmate_server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/notification")
    @Operation(summary = "푸시 알림 설정 조회", description = "현재 사용자의 푸시 알림 설정을 조회합니다.")
    public ResponseEntity<PushNotificationSettingResponse> getPushNotificationSetting(
            @AuthenticationPrincipal UserPrincipal principal) {

        PushNotificationSettingResponse response = userService.getPushNotificationSetting(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("notification")
    @Operation(summary = "푸시 알림 설정 수정", description = "사용자의 푸시 알림 설정을 수정합니다.")
    public ResponseEntity<PushNotificationSettingResponse> updatePushNotificationSetting(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PushNotificationSettingRequest request) {

        PushNotificationSettingResponse response = userService.updatePushNotificationSetting(
                principal.getUserId(),
                request
        );

        return ResponseEntity.ok(response);
    }
}
