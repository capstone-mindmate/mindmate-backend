package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import com.mindmate.mindmate_server.user.domain.User;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnreadConsumerTest {
    @Mock private ChatRoomService chatRoomService;
    @Mock private ChatPresenceService chatPresenceService;
    @Mock private ChatService chatService;

    @InjectMocks
    private UnreadCountConsumer unreadCountConsumer;

    private ChatMessageEvent mockEvent;
    private ChatRoom mockChatRoom;
    private ConsumerRecord<String, ChatMessageEvent> mockRecord;
    private User listenerUser;
    private User speakerUser;

    @BeforeEach
    void setup() {
        // Mock 객체 생성
        mockEvent = mock(ChatMessageEvent.class);
        mockChatRoom = mock(ChatRoom.class);
        mockRecord = mock(ConsumerRecord.class);
        ListenerProfile listenerProfile = mock(ListenerProfile.class);
        SpeakerProfile speakerProfile = mock(SpeakerProfile.class);
        listenerUser = mock(User.class);
        speakerUser = mock(User.class);

        Long messageId = 1L;
        Long roomId = 100L;
        Long listenerId = 1L;
        Long speakerId = 2L;

        when(mockEvent.getMessageId()).thenReturn(messageId);
        when(mockEvent.getRoomId()).thenReturn(roomId);

        when(listenerUser.getId()).thenReturn(listenerId);
        when(speakerUser.getId()).thenReturn(speakerId);

        when(listenerProfile.getUser()).thenReturn(listenerUser);
        when(speakerProfile.getUser()).thenReturn(speakerUser);

        when(mockChatRoom.getListener()).thenReturn(listenerProfile);
        when(mockChatRoom.getSpeaker()).thenReturn(speakerProfile);

        when(mockRecord.value()).thenReturn(mockEvent);
        when(chatRoomService.findChatRoomById(roomId)).thenReturn(mockChatRoom);
    }

    @Nested
    @DisplayName("UnreadCount 업데이트")
    class UpdateUnreadCountTest {
        @Test
        @DisplayName("리스너가 보낸 메시지 - 수신자 온라인")
        void updateUnreadCount_ListenerSender_RecipientOnline() {
            // given
            when(mockEvent.getSenderRole()).thenReturn(RoleType.ROLE_LISTENER);
            when(chatPresenceService.isUserActiveInRoom(anyLong(), anyLong())).thenReturn(true);

            // when
            unreadCountConsumer.updateUnreadCount(mockRecord);

            // then
            verify(mockChatRoom).markAsReadForListener(anyLong());
            verify(chatService).markAsRead(speakerUser.getId(), mockEvent.getRoomId());
            verify(chatPresenceService, never()).incrementUnreadCount(anyLong(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("리스너가 보낸 메시지 - 수신자 오프라인")
        void updateUnreadCount_ListenerSender_RecipientOffline() {
            // given
            when(mockEvent.getSenderRole()).thenReturn(RoleType.ROLE_LISTENER);
            when(chatPresenceService.isUserActiveInRoom(anyLong(), anyLong())).thenReturn(false);

            // when
            unreadCountConsumer.updateUnreadCount(mockRecord);

            // then
            verify(mockChatRoom).markAsReadForListener(anyLong());
            verify(chatService, never()).markAsRead(anyLong(), anyLong());
            verify(chatPresenceService).incrementUnreadCount(
                    mockEvent.getRoomId(),
                    speakerUser.getId(),
                    mockChatRoom,
                    RoleType.ROLE_LISTENER
            );
        }

        @Test
        @DisplayName("스피커가 보낸 메시지 - 수신자 온라인")
        void updateUnreadCount_SpeakerSender_RecipientOnline() {
            // given
            when(mockEvent.getSenderRole()).thenReturn(RoleType.ROLE_SPEAKER);
            when(chatPresenceService.isUserActiveInRoom(anyLong(), anyLong())).thenReturn(true);

            // when
            unreadCountConsumer.updateUnreadCount(mockRecord);

            // then
            verify(mockChatRoom).markAsReadForSpeaker(anyLong());
            verify(chatService).markAsRead(listenerUser.getId(), mockEvent.getRoomId());
            verify(chatPresenceService, never()).incrementUnreadCount(anyLong(), anyLong(), any(), any());
        }
    }
    @Test
    @DisplayName("예외 처리 테스트")
    void updateUnreadCount_ExceptionHandling() {
        // given
        when(chatRoomService.findChatRoomById(anyLong())).thenThrow(new RuntimeException("Test exception"));

        // when & then
        assertDoesNotThrow(() -> unreadCountConsumer.updateUnreadCount(mockRecord));
    }

}
