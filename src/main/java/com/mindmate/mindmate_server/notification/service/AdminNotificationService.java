package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.notification.dto.AnnouncementNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.AnnouncementNotificationRequest;
import com.mindmate.mindmate_server.notification.dto.NotificationSendResponse;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final NotificationService notificationService;
    private final UserService userService;

    @Transactional
    public NotificationSendResponse sendAnnouncementToAllUsers(AnnouncementNotificationRequest request) {
        try {
            AnnouncementNotificationEvent templateEvent = AnnouncementNotificationEvent.builder()
                    .recipientId(null)
                    .announcementTitle(request.getTitle())
                    .announcementContent(request.getContent())
                    .build();

            List<Long> allUserIds = userService.findAllUserIds();

            notificationService.sendNotificationToAllUsers(templateEvent, allUserIds);

            return NotificationSendResponse.builder()
                    .message("공지사항 알림이 모든 사용자에게 전송되었습니다.")
                    .success(true)
                    .sentCount(allUserIds.size())
                    .build();

        } catch (Exception e) {
            log.error("공지사항 알림 전송 중 오류 발생", e);
            return NotificationSendResponse.builder()
                    .message("공지사항 알림 전송 중 오류가 발생했습니다: " + e.getMessage())
                    .success(false)
                    .sentCount(0)
                    .build();
        }
    }
}