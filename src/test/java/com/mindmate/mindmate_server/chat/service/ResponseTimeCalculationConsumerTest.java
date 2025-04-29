package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.ProfileService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResponseTimeCalculationConsumerTest {
    @Mock private ProfileService profileService;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ConsumerRecord<String, ChatRoomCloseEvent> mockRecord;
    @Mock private ChatRoomCloseEvent mockEvent;

    @InjectMocks
    private ResponseTimeCalculationConsumer responseTimeCalculationConsumer;

    private final Long chatRoomId = 100L;
    private final Long speakerId = 1L;
    private final Long listenerId = 2L;

    @BeforeEach
    void setup() {
        when(mockRecord.value()).thenReturn(mockEvent);
        when(mockEvent.getChatRoomId()).thenReturn(chatRoomId);
        when(mockEvent.getSpeakerId()).thenReturn(speakerId);
        when(mockEvent.getListenerId()).thenReturn(listenerId);
    }

    @Test
    @DisplayName("응답 시간 계산 - 빈 메시지 목록")
    void calculateResponseTime_EmptyMessages() {
        // given
        when(chatMessageRepository.findByChatRoomIdOrderByIdAsc(eq(chatRoomId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // when
        responseTimeCalculationConsumer.calculateResponseTime(mockRecord);

        // then
        verify(chatMessageRepository).findByChatRoomIdOrderByIdAsc(eq(chatRoomId), any(Pageable.class));
        verify(profileService, never()).updateResponseTimes(anyLong(), anyList());
    }

    @Test
    @DisplayName("응답 시간 계산 - 단일 배치 메시지")
    void calculateResponseTime_SingleBatch() {
        // given
        List<ChatMessage> messages = createMessages(3);

        when(chatMessageRepository.findByChatRoomIdOrderByIdAsc(eq(chatRoomId), any(Pageable.class)))
                .thenReturn(messages);

        // when
        responseTimeCalculationConsumer.calculateResponseTime(mockRecord);

        // then
        verify(chatMessageRepository).findByChatRoomIdOrderByIdAsc(eq(chatRoomId), any(Pageable.class));
        verify(profileService).updateResponseTimes(eq(speakerId), any());
        verify(profileService).updateResponseTimes(eq(listenerId), any());
    }

    @Test
    @DisplayName("응답 시간 계산 - 다중 배치")
    void calculateResponseTime_MultipleBatches() {
        // given
        List<ChatMessage> firstBatch = createMessages(500, 1);
        List<ChatMessage> secondBatch = createMessages(3, 501);

        when(firstBatch.get(499).getId()).thenReturn(500L);
        when(secondBatch.get(2).getId()).thenReturn(503L);

        when(chatMessageRepository.findByChatRoomIdOrderByIdAsc(eq(chatRoomId), any(Pageable.class)))
                .thenReturn(firstBatch);
        when(chatMessageRepository.findByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                eq(chatRoomId), eq(500L), any(Pageable.class)))
                .thenReturn(secondBatch);

        // when
        responseTimeCalculationConsumer.calculateResponseTime(mockRecord);

        // then
        verify(chatMessageRepository).findByChatRoomIdOrderByIdAsc(eq(chatRoomId), any(Pageable.class));
        verify(chatMessageRepository).findByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                eq(chatRoomId), eq(500L), any(Pageable.class));

        verify(profileService).updateResponseTimes(eq(speakerId), any());
        verify(profileService).updateResponseTimes(eq(listenerId), any());
    }

    @Test
    @DisplayName("비합리적인 응답 시간은 제외")
    void calculateResponseTime_ExcludeUnreasonableTimes() {
        // given
        List<ChatMessage> messages = new ArrayList<>();

        // 첫 번째 메시지 (스피커)
        messages.add(createMessage(1L, speakerId, LocalDateTime.now().minusHours(2)));

        // 두 번째 메시지 (리스너) - 하루 이상 차이나는 비합리적인 응답 시간
        messages.add(createMessage(2L, listenerId, LocalDateTime.now().plusDays(2)));

        when(chatMessageRepository.findByChatRoomIdOrderByIdAsc(eq(chatRoomId), any(Pageable.class)))
                .thenReturn(messages);

        // when
        responseTimeCalculationConsumer.calculateResponseTime(mockRecord);

        // then
        verify(profileService, never()).updateResponseTimes(eq(speakerId), argThat(list -> !list.isEmpty()));
        verify(profileService, never()).updateResponseTimes(eq(listenerId), any());
    }

    private List<ChatMessage> createMessages(int count) {
        return createMessages(count, 1, 10);
    }

    private List<ChatMessage> createMessages(int count, int startId) {
        return createMessages(count, startId, 10);
    }

    private List<ChatMessage> createMessages(int count, int startId, int minutesInterval) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            Long id = (long) (startId + i);
            Long senderId = (i % 2 == 0) ? speakerId : listenerId; // 번갈아가며 메시지 전송
            LocalDateTime time = baseTime.plusMinutes(i * minutesInterval);

            ChatMessage message = mock(ChatMessage.class);
            User sender = mock(User.class);

            when(message.getId()).thenReturn(id);
            when(message.getSender()).thenReturn(sender);
            when(sender.getId()).thenReturn(senderId);
            when(message.getCreatedAt()).thenReturn(time);

            messages.add(message);
        }

        return messages;
    }

    private ChatMessage createMessage(Long id, Long senderId, LocalDateTime createdAt) {
        ChatMessage message = mock(ChatMessage.class);
        User sender = mock(User.class);

        when(message.getId()).thenReturn(id);
        when(message.getSender()).thenReturn(sender);
        when(sender.getId()).thenReturn(senderId);
        when(message.getCreatedAt()).thenReturn(createdAt);

        return message;
    }
}
