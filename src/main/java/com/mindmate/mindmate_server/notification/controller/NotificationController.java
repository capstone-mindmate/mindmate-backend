package com.mindmate.mindmate_server.notification.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.notification.dto.NotificationResponse;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "알림 관련 API")
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(summary = "사용자 알림 목록 조회", description = "사용자의 모든 알림을 페이지 단위로 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {

        Page<NotificationResponse> notifications =
                notificationService.getUserNotifications(principal.getUserId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "읽지 않은 알림 목록 조회", description = "사용자의 읽지 않은 알림 목록을 조회합니다.")
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<NotificationResponse> notifications =
                notificationService.getUserUnreadNotifications(principal.getUserId());
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "알림 읽음 표시", description = "특정 알림을 읽음으로 표시합니다.")
    @PutMapping("/{notificationId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId) {

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "모든 알림 읽음 표시", description = "사용자의 모든 알림을 읽음으로 표시합니다.")
    @PutMapping("/all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {

        notificationService.markAllAsRead(principal.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId) {

        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "모든 알림 삭제", description = "사용자의 모든 알림을 삭제합니다.")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {

        notificationService.deleteAllNotifications(principal.getUserId());
        return ResponseEntity.ok().build();
    }
}