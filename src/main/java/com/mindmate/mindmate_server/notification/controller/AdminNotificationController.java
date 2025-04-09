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

// 나중에 관리자 페이지로 옮겨도 될듯 + 관리자만 접근 가능하게 만들기
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @PostMapping("/announcement")
    public ResponseEntity<NotificationSendResponse> sendAnnouncementToAllUsers(
            @Valid @RequestBody AnnouncementNotificationRequest request) {

        NotificationSendResponse response = adminNotificationService.sendAnnouncementToAllUsers(request);
        return ResponseEntity.ok(response);
    }
}