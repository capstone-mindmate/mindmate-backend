package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.notification.dto.AnnouncementNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.AnnouncementNotificationRequest;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.dto.NotificationSendResponse;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminNotificationService adminNotificationService;

    private AnnouncementNotificationRequest request;
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        request = AnnouncementNotificationRequest.builder()
                .title("공지사항 제목")
                .content("공지 내용")
                .build();

        userIds = Arrays.asList(1L, 2L, 3L);
    }

    @Test
    @DisplayName("모든 사용자에게 공지사항 알림 전송 성공")
    void sendAnnouncementToAllUsers_shouldSendToAllUsers() {
        // given
        when(userService.findAllUserIds()).thenReturn(userIds);

        // when
        NotificationSendResponse response = adminNotificationService.sendAnnouncementToAllUsers(request);

        // then
        verify(userService).findAllUserIds();
        verify(notificationService).sendNotificationToAllUsers(any(AnnouncementNotificationEvent.class), eq(userIds));

        assertTrue(response.isSuccess());
        assertEquals(3, response.getSentCount());
        assertEquals("공지사항 알림이 모든 사용자에게 전송되었습니다.", response.getMessage());
    }

    @Test
    @DisplayName("공지사항 알림 전송 중 예외 발생")
    void sendAnnouncementToAllUsers_shouldHandleException() {
        // given
        when(userService.findAllUserIds()).thenThrow(new RuntimeException("사용자 목록 조회 실패"));

        // when
        NotificationSendResponse response = adminNotificationService.sendAnnouncementToAllUsers(request);

        // then
        verify(userService).findAllUserIds();
        verify(notificationService, never()).sendNotificationToAllUsers(any(), any());

        assertEquals(false, response.isSuccess());
        assertEquals(0, response.getSentCount());
        assertTrue(response.getMessage().contains("공지사항 알림 전송 중 오류가 발생했습니다"));
    }

    @Test
    @DisplayName("공지사항 템플릿 이벤트 생성 확인")
    void sendAnnouncementToAllUsers_shouldCreateCorrectTemplateEvent() {
        // given
        when(userService.findAllUserIds()).thenReturn(userIds);
        ArgumentCaptor<AnnouncementNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AnnouncementNotificationEvent.class);

        // when
        adminNotificationService.sendAnnouncementToAllUsers(request);

        // then
        verify(notificationService).sendNotificationToAllUsers(eventCaptor.capture(), eq(userIds));

        AnnouncementNotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(null, capturedEvent.getRecipientId());
        assertEquals("공지 내용", capturedEvent.getContent());
        assertEquals("공지사항 제목", capturedEvent.getAnnouncementTitle());
    }
}