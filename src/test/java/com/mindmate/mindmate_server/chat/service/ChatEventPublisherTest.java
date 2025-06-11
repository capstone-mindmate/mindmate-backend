package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.dto.ChatEventType;
import com.mindmate.mindmate_server.chat.util.WebSocketDestinationResolver;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatEventPublisherTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private WebSocketDestinationResolver webSocketDestinationResolver;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatEventPublisher chatEventPublisher;

    private static final Long ROOM_ID = 123L;
    private static final Long USER_ID = 99L;
    private static final String CHANNEL = "chat:room:123";
    private static final String USER_CHANNEL = "user:status:99";
    private static final String EVENT_JSON = "{\"type\":\"MESSAGE\",\"data\":\"hello\"}";
    private static final String DESTINATION = "/topic/chat.room.123";

    @Test
    @DisplayName("채팅방 이벤트 정상 발행")
    void publishChatRoomEvent_success() throws Exception {
        // given
        when(redisKeyManager.getChatRoomChannel(ROOM_ID)).thenReturn(CHANNEL);
        when(objectMapper.writeValueAsString(any())).thenReturn(EVENT_JSON);

        // when
        chatEventPublisher.publishChatRoomEvent(ROOM_ID, ChatEventType.MESSAGE, "hello");

        // then
        verify(redisTemplate).convertAndSend(CHANNEL, EVENT_JSON);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("채팅방 이벤트 발행 - 직렬화 예외 발생 후 fallback WebSocket 전송")
    void publishChatRoomEvent_jsonException_fallbackToWebSocket() throws Exception {
        // given
        when(redisKeyManager.getChatRoomChannel(ROOM_ID)).thenReturn(CHANNEL);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("fail") {})
                .thenThrow(new JsonProcessingException("fail2") {})
                .thenThrow(new JsonProcessingException("fail3") {})
                .thenReturn("{\"data\":\"hello\"}");
        when(webSocketDestinationResolver.getDestinationByEventType(ROOM_ID.toString(), "MESSAGE"))
                .thenReturn(DESTINATION);

        // when
        chatEventPublisher.publishChatRoomEvent(ROOM_ID, ChatEventType.MESSAGE, "hello");

        // then
        verify(redisTemplate, times(0)).convertAndSend(any(), any());
        verify(messagingTemplate).convertAndSend(eq(DESTINATION), eq("{\"data\":\"hello\"}"));
    }


    @Test
    @DisplayName("채팅방 이벤트 발행 - fallback WebSocket도 실패해도 예외 발생 안함")
    void publishChatRoomEvent_fallbackWebSocketFailure_noException() throws Exception {
        // given
        when(redisKeyManager.getChatRoomChannel(ROOM_ID)).thenReturn(CHANNEL);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("fail") {})
                .thenThrow(new JsonProcessingException("fail2") {})
                .thenThrow(new JsonProcessingException("fail3") {});
        when(webSocketDestinationResolver.getDestinationByEventType(any(), any()))
                .thenReturn(DESTINATION);
        doThrow(new RuntimeException("WebSocket fail"))
                .when(messagingTemplate).convertAndSend(anyString(), anyString());

        // when & then
        assertDoesNotThrow(() ->
                chatEventPublisher.publishChatRoomEvent(ROOM_ID, ChatEventType.MESSAGE, "hello"));
    }

    @Test
    @DisplayName("유저 이벤트 정상 발행")
    void publishUserEvent_success() throws Exception {
        // given
        when(redisKeyManager.getUserStatusChannel(USER_ID)).thenReturn(USER_CHANNEL);
        when(objectMapper.writeValueAsString(any())).thenReturn(EVENT_JSON);

        // when
        chatEventPublisher.publishUserEvent(USER_ID, ChatEventType.MESSAGE, "hello");

        // then
        verify(redisTemplate).convertAndSend(USER_CHANNEL, EVENT_JSON);
    }

    @Test
    @DisplayName("유저 이벤트 발행 - 직렬화 예외 발생")
    void publishUserEvent_jsonException_logged() throws Exception {
        // given
        when(redisKeyManager.getUserStatusChannel(USER_ID)).thenReturn(USER_CHANNEL);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("fail") {});

        // when & then
        assertDoesNotThrow(() ->
                chatEventPublisher.publishUserEvent(USER_ID, ChatEventType.MESSAGE, "hello"));
    }
}
