//package com.mindmate.mindmate_server.chat.service;
//
//import com.mindmate.mindmate_server.chat.domain.ChatRoomCloseType;
//import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
//import com.mindmate.mindmate_server.chat.dto.ChatRoomNotificationEvent;
//import com.mindmate.mindmate_server.notification.service.NotificationService;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class ChatRoomCloseNotificationConsumerTest {
//
//    @Mock private NotificationService notificationService;
//    @Mock private ConsumerRecord<String, ChatRoomCloseEvent> mockRecord;
//    @Mock private ChatRoomCloseEvent mockEvent;
//
//    @InjectMocks
//    private ChatRoomCloseNotificationConsumer chatRoomCloseNotificationConsumer;
//
//    private final Long speakerId = 1L;
//    private final Long listenerId = 2L;
//    private final Long chatRoomId = 100L;
//
//    @BeforeEach
//    void setup() {
//        when(mockRecord.value()).thenReturn(mockEvent);
//        when(mockEvent.getSpeakerId()).thenReturn(speakerId);
//        when(mockEvent.getListenerId()).thenReturn(listenerId);
//        when(mockEvent.getChatRoomId()).thenReturn(chatRoomId);
//    }
//
//    @Test
//    @DisplayName("채팅방 종료 시 발신자 수신자 모두에게 알림 전송")
//    void sendCloseNotification_Success() {
//        // when
//        chatRoomCloseNotificationConsumer.sendCloseNotification(mockRecord);
//
//        // then
//        ArgumentCaptor<ChatRoomNotificationEvent> captor = ArgumentCaptor.forClass(ChatRoomNotificationEvent.class);
//        verify(notificationService, times(2)).processNotification(captor.capture());
//
//        List<ChatRoomNotificationEvent> capturedEvents = captor.getAllValues();
//        assertThat(capturedEvents).hasSize(2);
//
//        ChatRoomNotificationEvent speakerEvent = capturedEvents.get(0);
//        assertThat(speakerEvent.getRecipientId()).isEqualTo(speakerId);
//        assertThat(speakerEvent.getChatRoomId()).isEqualTo(chatRoomId);
//
//        ChatRoomNotificationEvent listenerEvent = capturedEvents.get(1);
//        assertThat(listenerEvent.getRecipientId()).isEqualTo(listenerId);
//        assertThat(listenerEvent.getChatRoomId()).isEqualTo(chatRoomId);
//
//        assertThat(listenerEvent.getCloseType()).isEqualTo(ChatRoomCloseType.ACCEPT);
//    }
//
//    @Test
//    @DisplayName("알림 처리 중 예외 발생")
//    void sendCloseNotification_ExceptionHandling() {
//        // given
//        doThrow(new RuntimeException("Test exception")).when(notificationService).processNotification(any());
//
//        // when
//        chatRoomCloseNotificationConsumer.sendCloseNotification(mockRecord);
//
//        // then
//        verify(notificationService).processNotification(any());
//    }
//
//
//}