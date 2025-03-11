//package com.mindmate.mindmate_server.chat.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mindmate.mindmate_server.chat.domain.ChatMessage;
//import com.mindmate.mindmate_server.chat.domain.ChatRoom;
//import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
//import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.redis.core.RedisTemplate;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class ContentFilterConsumerTest {
//    @Mock private ContentFilterService contentFilterService;
//    @Mock private ChatMessageService chatMessageService;
//    @Mock private RedisTemplate<String, Object> redisTemplate;
//    @Mock private ObjectMapper objectMapper;
//
//    @InjectMocks
//    private ContentFilterConsumer contentFilterConsumer;
//
//    private ChatMessageEvent mockEvent;
//    private ChatMessage mockMessage;
//    private ConsumerRecord<String, ChatMessageEvent> mockRecord;
//
//    @BeforeEach
//    void setup() throws JsonProcessingException {
//        mockEvent = mock(ChatMessageEvent.class);
//        mockMessage = mock(ChatMessage.class);
//        ChatRoom mockChatRoom = mock(ChatRoom.class);
//        mockRecord = mock(ConsumerRecord.class);
//
//        Long messageId = 1L;
//        Long roomId = 100L;
//        String content = "테스트 내용";
//
//        when(mockEvent.getMessageId()).thenReturn(messageId);
//        when(mockEvent.getRoomId()).thenReturn(roomId);
//        when(mockEvent.getContent()).thenReturn(content);
//
//        when(mockMessage.getId()).thenReturn(messageId);
//        when(mockMessage.getContent()).thenReturn(content);
//        when(mockMessage.getChatRoom()).thenReturn(mockChatRoom);
//
//        when(mockChatRoom.getId()).thenReturn(roomId);
//
//        when(mockRecord.value()).thenReturn(mockEvent);
//
//        when(chatMessageService.findChatMessageById(messageId)).thenReturn(mockMessage);
//        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
//    }
//
//    @Test
//    @DisplayName("금지어 없는 메시지 필터링")
//    void filterContent_BoFilteringWord() {
//        // given
//        when(contentFilterService.findFilteringWordCategory(anyString())).thenReturn(Optional.empty());
//
//        // when
//        contentFilterConsumer.filterContent(mockRecord);
//
//        // then
//        verify(contentFilterService).findFilteringWordCategory(anyString());
//        verify(mockMessage, never()).setFilteredContent(anyString());
//    }
//
//    @Test
//    @DisplayName("금지어 포함 메시지 필터링")
//    void filterContent_WithFilteringWord() throws JsonProcessingException {
//        // given
//        FilteringWordCategory mockCategory = mock(FilteringWordCategory.class);
//        when(mockCategory.getDescription()).thenReturn("욕설");
//        when(contentFilterService.findFilteringWordCategory(anyString())).thenReturn(Optional.of(mockCategory));
//
//        // when
//        contentFilterConsumer.filterContent(mockRecord);
//
//        // then
//        verify(contentFilterService).findFilteringWordCategory(anyString());
//        verify(mockMessage).setFilteredContent(contains("욕설"));
//        verify(redisTemplate).convertAndSend(anyString(), anyString());
//    }
//
//    @Test
//    @DisplayName("예외 처리 테스트")
//    void filterContent_ExceptionHandling() {
//        // given
//        when(chatMessageService.findChatMessageById(anyLong())).thenThrow(new RuntimeException("Test exception"));
//
//        // when & then
//        assertDoesNotThrow(() -> contentFilterConsumer.filterContent(mockRecord));
//    }
//}
