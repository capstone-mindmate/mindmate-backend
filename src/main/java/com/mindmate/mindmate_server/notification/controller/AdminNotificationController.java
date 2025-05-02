package com.mindmate.mindmate_server.notification.controller;

import com.mindmate.mindmate_server.notification.dto.AnnouncementNotificationRequest;
import com.mindmate.mindmate_server.notification.dto.NotificationSendResponse;
import com.mindmate.mindmate_server.notification.service.AdminNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

// 나중에 관리자 페이지로 옮겨도 될듯 + 관리자만 접근 가능하게 만들기
@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "관리자 알림 관련 API")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @Operation(summary = "공지사항 알림 전송", description = "모든 사용자에게 공지사항 알림을 전송합니다.")
    @PostMapping("/announcement")
    public ResponseEntity<NotificationSendResponse> sendAnnouncementToAllUsers(
            @Valid @RequestBody AnnouncementNotificationRequest request) {

        NotificationSendResponse response = adminNotificationService.sendAnnouncementToAllUsers(request);
        return ResponseEntity.ok(response);
    }
}