package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.support.Acknowledgment;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatNotificationConsumerTest {
    @Mock private NotificationService notificationService;
    @Mock private UserService userService;
    @Mock private ConsumerRecord<String, ChatMessageEvent> mockRecord;
    @Mock private ChatMessageEvent mockEvent;
    @Mock private User mockSender;
    @Mock private User mockRecipient;
    @Mock private Profile mockSenderProfile;
    @Mock private Acknowledgment acknowledgment;


    @InjectMocks
    private ChatNotificationConsumer chatNotificationConsumer;

    private final Long senderId = 1L;
    private final Long recipientId = 2L;
    private final Long roomId = 100L;
    private final Long messageId = 1000L;
    private final String senderNickname = "테스트 발신자";
    private final String messageContent = "테스트 메시지";

    @BeforeEach
    void setup() {
        when(mockRecord.value()).thenReturn(mockEvent);
        when(mockEvent.getMessageId()).thenReturn(messageId);
        when(mockEvent.getSenderId()).thenReturn(senderId);
        when(mockEvent.getRecipientId()).thenReturn(recipientId);
        when(mockEvent.getRoomId()).thenReturn(roomId);

        when(mockSender.getId()).thenReturn(senderId);
        when(mockSender.getProfile()).thenReturn(mockSenderProfile);
        when(mockSenderProfile.getNickname()).thenReturn(senderNickname);

        when(mockRecipient.getId()).thenReturn(recipientId);
        when(userService.findUserById(senderId)).thenReturn(mockSender);
        when(userService.findUserById(recipientId)).thenReturn(mockRecipient);
    }

    @ParameterizedTest
    @DisplayName("알림 처리를 하지 않는 조건 테스트")
    @MethodSource("noProcessingScenarios")
    void sendNotification_ShouldNotProcess(
            String testcase,
            boolean isFiltered,
            Long messageId,
            boolean isRecipientActive) {
        // given
        when(mockEvent.isFiltered()).thenReturn(isFiltered);
        when(mockEvent.getMessageId()).thenReturn(messageId);
        when(mockEvent.isRecipientActive()).thenReturn(isRecipientActive);

        // when
        chatNotificationConsumer.sendNotification(mockRecord, acknowledgment);

        // then
        verify(userService, never()).findUserById(anyLong());
        verify(notificationService, never()).processNotification(any());
        verify(acknowledgment).acknowledge();
    }

    static Stream<Arguments> noProcessingScenarios() {
        return Stream.of(
                Arguments.of("필터링된 메시지", true, 1L, false),
                Arguments.of("메시지 ID가 null", false, null, false),
                Arguments.of("수신자가 활성 상태", false, 1L, true)
        );
    }

    @ParameterizedTest
    @DisplayName("메시지 타입별 알림 내용 처리")
    @MethodSource("messageTypeScenarios")
    void sendNotification_DifferentMessageTypes(
            MessageType messageType,
            String expectedContent) {
        // given
        when(mockEvent.isFiltered()).thenReturn(false);
        when(mockEvent.isRecipientActive()).thenReturn(false);
        when(mockEvent.getType()).thenReturn(messageType);
        when(mockEvent.getPlainContent()).thenReturn(messageContent);

        // when
        chatNotificationConsumer.sendNotification(mockRecord, acknowledgment);

        // then
        ArgumentCaptor<ChatMessageNotificationEvent> captor = ArgumentCaptor.forClass(ChatMessageNotificationEvent.class);
        verify(notificationService).processNotification(captor.capture());

        ChatMessageNotificationEvent capturedEvent = captor.getValue();
        assertThat(capturedEvent.getRecipientId()).isEqualTo(recipientId);
        assertThat(capturedEvent.getSenderId()).isEqualTo(senderId);
        assertThat(capturedEvent.getSenderName()).isEqualTo(senderNickname);
        assertThat(capturedEvent.getRoomId()).isEqualTo(roomId);
        assertThat(capturedEvent.getMessageId()).isEqualTo(messageId);
        assertThat(capturedEvent.getMessageContent()).isEqualTo(expectedContent);

        verify(acknowledgment).acknowledge();
    }
    static Stream<Arguments> messageTypeScenarios() {
        return Stream.of(
                Arguments.of(MessageType.TEXT, "테스트 메시지"),
                Arguments.of(MessageType.CUSTOM_FORM, "커스텀폼 메시지가 도착했습니다."),
                Arguments.of(MessageType.EMOTICON, "이모티콘이 도착했습니다."),
                Arguments.of(MessageType.IMAGE, "새 메시지가 도착했습니다.")
        );
    }
}