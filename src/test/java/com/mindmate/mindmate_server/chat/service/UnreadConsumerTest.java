//package com.mindmate.mindmate_server.chat.service;
//
//import com.mindmate.mindmate_server.chat.domain.ChatRoom;
//import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
//import com.mindmate.mindmate_server.user.domain.User;
//import com.mindmate.mindmate_server.user.service.UserService;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class UnreadConsumerTest {
//    @Mock private ChatRoomService chatRoomService;
//    @Mock private ChatPresenceService chatPresenceService;
//    @Mock private ChatService chatService;
//    @Mock private UserService userService;
//
//
//    @InjectMocks
//    private UnreadCountConsumer unreadCountConsumer;
//
//    private ChatMessageEvent mockEvent;
//    private ChatRoom mockChatRoom;
//    private ConsumerRecord<String, ChatMessageEvent> mockRecord;
//    private User mockListener;
//    private User mockSpeaker;
//
//    private final Long messageId = 1L;
//    private final Long roomId = 100L;
//    private final Long listenerId = 1L;
//    private final Long speakerId = 2L;
//
//    @BeforeEach
//    void setup() {
//        // Mock 객체 생성
//        mockEvent = mock(ChatMessageEvent.class);
//        mockChatRoom = mock(ChatRoom.class);
//        mockRecord = mock(ConsumerRecord.class);
//        mockListener = mock(User.class);
//        mockSpeaker = mock(User.class);
//
//        when(mockEvent.getMessageId()).thenReturn(messageId);
//        when(mockEvent.getRoomId()).thenReturn(roomId);
//        when(mockEvent.isFiltered()).thenReturn(false);
//
//        when(mockListener.getId()).thenReturn(listenerId);
//        when(mockSpeaker.getId()).thenReturn(speakerId);
//
//        when(mockChatRoom.getListener()).thenReturn(mockListener);
//        when(mockChatRoom.getSpeaker()).thenReturn(mockSpeaker);
//
//        when(mockRecord.value()).thenReturn(mockEvent);
//        when(chatRoomService.findChatRoomById(roomId)).thenReturn(mockChatRoom);
//        when(userService.findUserById(listenerId)).thenReturn(mockListener);
//        when(userService.findUserById(speakerId)).thenReturn(mockSpeaker);
//    }
//
//    @Nested
//    @DisplayName("UnreadCount 업데이트")
//    class UpdateUnreadCountTest {
//
//        @ParameterizedTest
//        @DisplayName("메시지 수신 시나리오")
//        @MethodSource("messageScenarios")
//        void updateUnreadCount_Scenarios(
//                boolean senderIsListener,
//                boolean recipientActive,
//                boolean expectMarkAsRead,
//                boolean expectIncrementUnread,
//                boolean expectIncreaseForListener) {
//            // given
//            User sender = senderIsListener ? mockListener : mockSpeaker;
//            Long senderId = senderIsListener ? listenerId : speakerId;
//            User recipient = senderIsListener ? mockSpeaker : mockListener;
//            Long recipientId = senderIsListener ? speakerId : listenerId;
//
//            when(mockEvent.getSenderId()).thenReturn(senderId);
//            when(userService.findUserById(senderId)).thenReturn(sender);
//            when(mockChatRoom.isListener(sender)).thenReturn(senderIsListener);
//            when(mockChatRoom.isSpeaker(sender)).thenReturn(!senderIsListener);
//
//            when(mockEvent.getRecipientId()).thenReturn(recipientId);
//            when(mockEvent.isRecipientActive()).thenReturn(recipientActive);
//
//            when(mockChatRoom.isListener(recipient)).thenReturn(!senderIsListener);
//            when(mockChatRoom.isSpeaker(recipient)).thenReturn(senderIsListener);
//            when(chatPresenceService.incrementUnreadCountInRedis(anyLong(), anyLong())).thenReturn(1L);
//
//            // when
//            unreadCountConsumer.updateUnreadCount(mockRecord);
//
//            // then
//            verify(mockChatRoom).markAsRead(sender, messageId);
//
//            if (expectMarkAsRead) {
//                verify(chatService).markAsRead(recipientId, roomId);
//            } else {
//                verify(chatService, never()).markAsRead(anyLong(), anyLong());
//            }
//
//            if (expectIncrementUnread) {
//                verify(chatPresenceService).incrementUnreadCountInRedis(roomId, recipientId);
//                verify(chatRoomService).save(mockChatRoom);
//
//                if (expectIncreaseForListener) {
//                    verify(mockChatRoom).increaseUnreadCountForListener();
//                    verify(mockChatRoom, never()).increaseUnreadCountForSpeaker();
//                } else {
//                    verify(mockChatRoom).increaseUnreadCountForSpeaker();
//                    verify(mockChatRoom, never()).increaseUnreadCountForListener();
//                }
//            } else {
//                verify(chatPresenceService, never()).incrementUnreadCountInRedis(anyLong(), anyLong());
//                verify(mockChatRoom, never()).increaseUnreadCountForSpeaker();
//                verify(mockChatRoom, never()).increaseUnreadCountForListener();
//            }
//        }
//
//        static Stream<Arguments> messageScenarios() {
//            return Stream.of(
//                    // senderIsListener, recipientActive, expectMarkAsRead, expectIncrementUnread, expectIncreaseForListener
//                    Arguments.of(true, true, true, false, false),   // 리스너가 보냄, 스피커가 온라인
//                    Arguments.of(true, false, false, true, false),  // 리스너가 보냄, 스피커가 오프라인
//                    Arguments.of(false, true, true, false, false),  // 스피커가 보냄, 리스너가 온라인
//                    Arguments.of(false, false, false, true, true)   // 스피커가 보냄, 리스너가 오프라인
//            );
//        }
//    }
//
//    @Test
//    @DisplayName("필터링된 메시지 처리 x")
//    void updateUnreadCount_FilteringMessage() {
//        // given
//        when(mockEvent.isFiltered()).thenReturn(true);
//
//        // when
//        unreadCountConsumer.updateUnreadCount(mockRecord);
//
//        // then
//        verify(chatRoomService, never()).findChatRoomById(anyLong());
//        verify(userService, never()).findUserById(anyLong());
//        verify(mockChatRoom, never()).markAsRead(any(), anyLong());
//    }
//
//    @Test
//    @DisplayName("메시지 ID가 null이면 처리 x")
//    void updateUnreadCount_NullMessageId() {
//        // given
//        when(mockEvent.getMessageId()).thenReturn(null);
//
//        // when
//        unreadCountConsumer.updateUnreadCount(mockRecord);
//
//        // then
//        verify(chatRoomService, never()).findChatRoomById(anyLong());
//        verify(userService, never()).findUserById(anyLong());
//        verify(mockChatRoom, never()).markAsRead(any(), anyLong());
//    }
//
//    @Test
//    @DisplayName("예외 처리 테스트")
//    void updateUnreadCount_ExceptionHandling() {
//        // given
//        when(mockEvent.getSenderId()).thenReturn(listenerId);
//        when(userService.findUserById(listenerId)).thenReturn(mockListener);
//        when(chatRoomService.findChatRoomById(anyLong())).thenThrow(new RuntimeException("Test exception"));
//
//        // when & then
//        assertDoesNotThrow(() -> unreadCountConsumer.updateUnreadCount(mockRecord));
//    }
//
//}
