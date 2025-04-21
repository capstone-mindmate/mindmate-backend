package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
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
    @Mock private UserService userService;


    @InjectMocks
    private UnreadCountConsumer unreadCountConsumer;

    private ChatMessageEvent mockEvent;
    private ChatRoom mockChatRoom;
    private ConsumerRecord<String, ChatMessageEvent> mockRecord;
    private User mockSender;
    private User mockListener;
    private User mockSpeaker;

    @BeforeEach
    void setup() {
        // Mock 객체 생성
        mockEvent = mock(ChatMessageEvent.class);
        mockChatRoom = mock(ChatRoom.class);
        mockRecord = mock(ConsumerRecord.class);
        mockSender = mock(User.class);
        mockListener = mock(User.class);
        mockSpeaker = mock(User.class);

        Long messageId = 1L;
        Long roomId = 100L;
        Long senderId = 1L;
        Long listenerId = 1L;
        Long speakerId = 2L;

        when(mockEvent.getMessageId()).thenReturn(messageId);
        when(mockEvent.getRoomId()).thenReturn(roomId);
        when(mockEvent.getSenderId()).thenReturn(senderId);

        when(mockListener.getId()).thenReturn(listenerId);
        when(mockSpeaker.getId()).thenReturn(speakerId);

        when(mockChatRoom.getListener()).thenReturn(mockListener);
        when(mockChatRoom.getSpeaker()).thenReturn(mockSpeaker);

        when(mockRecord.value()).thenReturn(mockEvent);
        when(chatRoomService.findChatRoomById(roomId)).thenReturn(mockChatRoom);
        when(userService.findUserById(senderId)).thenReturn(mockSender);
        when(chatRoomService.findChatRoomById(mockChatRoom.getId())).thenReturn(mockChatRoom);
    }

    @Nested
    @DisplayName("UnreadCount 업데이트")
    class UpdateUnreadCountTest {
        @Test
        @DisplayName("리스너가 보낸 메시지 - 수신자 온라인")
        void updateUnreadCount_ListenerSender_RecipientOnline() {
            // given
            when(mockChatRoom.isListener(mockSender)).thenReturn(true);
            when(chatPresenceService.isUserActiveInRoom(anyLong(), anyLong())).thenReturn(true);

            // when
            unreadCountConsumer.updateUnreadCount(mockRecord);

            // then
            verify(mockChatRoom).markAsRead(mockSender, mockEvent.getMessageId());
            verify(chatService).markAsRead(mockSpeaker.getId(), mockEvent.getRoomId());
            verify(chatPresenceService, never()).incrementUnreadCountInRedis(anyLong(), anyLong());
            verify(mockChatRoom, never()).increaseUnreadCountForListener();
            verify(mockChatRoom, never()).increaseUnreadCountForSpeaker();
        }

        @Test
        @DisplayName("리스너가 보낸 메시지 - 수신자 오프라인")
        void updateUnreadCount_ListenerSender_RecipientOffline() {
            // given
            when(mockChatRoom.isListener(mockSender)).thenReturn(true);
            when(chatPresenceService.isUserActiveInRoom(anyLong(), anyLong())).thenReturn(false);
            when(chatPresenceService.incrementUnreadCountInRedis(anyLong(), anyLong())).thenReturn(1L);
            when(mockChatRoom.isListener(mockSpeaker)).thenReturn(false);

            // when
            unreadCountConsumer.updateUnreadCount(mockRecord);

            // then
            verify(mockChatRoom).markAsRead(mockSender, mockEvent.getMessageId());
            verify(chatService, never()).markAsRead(anyLong(), anyLong());
            verify(chatPresenceService).incrementUnreadCountInRedis(mockEvent.getRoomId(), mockSpeaker.getId());
            verify(mockChatRoom).increaseUnreadCountForSpeaker();
            verify(chatRoomService).save(mockChatRoom);
        }

        @Test
        @DisplayName("스피커가 보낸 메시지 - 수신자 온라인")
        void updateUnreadCount_SpeakerSender_RecipientOnline() {
            // given
            when(mockChatRoom.isListener(mockSender)).thenReturn(false);
            when(mockChatRoom.isSpeaker(mockSender)).thenReturn(true);
            when(chatPresenceService.isUserActiveInRoom(anyLong(), anyLong())).thenReturn(true);

            // when
            unreadCountConsumer.updateUnreadCount(mockRecord);

            // then
            verify(mockChatRoom).markAsRead(mockSender, mockEvent.getMessageId());
            verify(chatService).markAsRead(mockListener.getId(), mockEvent.getRoomId());
            verify(chatPresenceService, never()).incrementUnreadCountInRedis(anyLong(), anyLong());
            verify(mockChatRoom, never()).increaseUnreadCountForListener();
            verify(mockChatRoom, never()).increaseUnreadCountForSpeaker();
        }

        @Test
        @DisplayName("스피커가 보낸 메시지 - 수신자 오프라인")
        void updateUnreadCount_SpeakerSender_RecipientOffline() {
            // given
            when(mockChatRoom.isListener(mockSender)).thenReturn(false);
            when(mockChatRoom.isSpeaker(mockSender)).thenReturn(true);
            when(chatPresenceService.isUserActiveInRoom(anyLong(), anyLong())).thenReturn(false);
            when(chatPresenceService.incrementUnreadCountInRedis(anyLong(), anyLong())).thenReturn(1L);
            when(mockChatRoom.isListener(mockListener)).thenReturn(true);

            // when
            unreadCountConsumer.updateUnreadCount(mockRecord);

            // then
            verify(mockChatRoom).markAsRead(mockSender, mockEvent.getMessageId());
            verify(chatService, never()).markAsRead(anyLong(), anyLong());
            verify(chatPresenceService).incrementUnreadCountInRedis(mockEvent.getRoomId(), mockListener.getId());
            verify(mockChatRoom).increaseUnreadCountForListener();
            verify(chatRoomService).save(mockChatRoom);
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
