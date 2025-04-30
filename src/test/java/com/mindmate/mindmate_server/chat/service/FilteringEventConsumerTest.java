package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.service.AdminUserSuspensionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilteringEventConsumerTest {
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private AdminUserSuspensionService suspensionService;
    @Mock private ConsumerRecord<String, ChatMessageEvent> mockRecord;
    @Mock private ChatMessageEvent mockEvent;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ListOperations<String, Object> listOperations;

    @InjectMocks
    private FilteringEventConsumer filteringEventConsumer;

    private final Long senderId = 1L;
    private final Long roomId = 100L;
    private final String content = "필터링된 내용";
    private final String filteringCountKey = "filtering:count:1:100";
    private final String filteringContentKey = "filtering:content:1:100";

    @BeforeEach
    void setup() {
        when(mockRecord.value()).thenReturn(mockEvent);
        when(mockEvent.getSenderId()).thenReturn(senderId);
        when(mockEvent.getRoomId()).thenReturn(roomId);
        when(mockEvent.getContent()).thenReturn(content);

        when(redisKeyManager.getFilteringCountKey(senderId, roomId)).thenReturn(filteringCountKey);
        when(redisKeyManager.getFilteringContentKey(senderId, roomId)).thenReturn(filteringContentKey);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @ParameterizedTest
    @DisplayName("처리하지 않아야 하는 메시지 시나리오")
    @MethodSource("noProcessingScenarios")
    void processFilteringEvent_ShouldNotProcess(
            String testCase,
            boolean isFiltered,
            Long senderId) {
        // given
        when(mockEvent.isFiltered()).thenReturn(isFiltered);
        when(mockEvent.getSenderId()).thenReturn(senderId);

        // when
        filteringEventConsumer.processFilteringEvent(mockRecord);

        // then
        verify(redisTemplate, never()).opsForValue();
        verify(redisTemplate, never()).opsForList();
    }

    static Stream<Arguments> noProcessingScenarios() {
        return Stream.of(
                Arguments.of("필터링되지 않은 메시지", false, 1L),
                Arguments.of("발신자 ID가 null인 경우", true, null)
        );
    }

    @ParameterizedTest
    @DisplayName("필터링 횟수에 따른 처리")
    @ValueSource(longs = {1L, 4L, 5L})
    void processFilteringEvent_DifferentCounts(Long count) {
        // given
        when(mockEvent.isFiltered()).thenReturn(true);
        when(valueOperations.increment(filteringCountKey, 1)).thenReturn(count);

        // when
        filteringEventConsumer.processFilteringEvent(mockRecord);

        // then
        verify(valueOperations).increment(filteringCountKey, 1);
        verify(listOperations).leftPush(eq(filteringContentKey), anyString());
        verify(listOperations).trim(filteringContentKey, 0 ,4);
        verify(redisTemplate).expire(filteringContentKey, 24, TimeUnit.HOURS);

        if (count == 1L) {
            verify(redisTemplate).expire(filteringCountKey, 24, TimeUnit.HOURS);
        }

        if (count >= 5L) {
            verify(suspensionService).suspendUser(
                    eq(senderId),
                    eq(-1),
                    eq(Duration.ofHours(2)),
                    eq("채팅 필터링 위반 (5회 이상)")
            );
            verify(redisTemplate).delete(filteringCountKey);
        } else {
            verify(suspensionService, never()).suspendUser(anyLong(), anyInt(), any(), anyString());
            verify(redisTemplate, never()).delete(filteringCountKey);
        }
    }

    @Test
    @DisplayName("긴 내용 잘라내기")
    void processFilteringEvent_TruncateLongContent() {
        // given
        when(mockEvent.isFiltered()).thenReturn(true);
        when(valueOperations.increment(filteringCountKey, 1)).thenReturn(1L);

        String longContent = "a".repeat(150);
        when(mockEvent.getContent()).thenReturn(longContent);

        // when
        filteringEventConsumer.processFilteringEvent(mockRecord);

        // then
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).leftPush(eq(filteringContentKey), contentCaptor.capture());

        String capturedContent = contentCaptor.getValue();
        assertThat(capturedContent).hasSize(103);
        assertThat(capturedContent).endsWith("...");
    }


}