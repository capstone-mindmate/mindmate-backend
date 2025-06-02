package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.magazine.dto.MagazineEngagementEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazineEngagementConsumerTest {

    @Mock private MagazinePopularityService popularityService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private Acknowledgment acknowledgment;

    @InjectMocks
    private MagazineEngagementConsumer consumer;

    private MagazineEngagementEvent event;
    private long normalizedTime;
    private String eventId;
    private String processedKey;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        event = MagazineEngagementEvent.builder()
                .userId(1L)
                .magazineId(100L)
                .dwellTime(5000L)
                .scrollPercentage(80.0)
                .timestamp(Instant.ofEpochMilli(3_600_000)) // 1시간 (3600000ms)
                .build();

        normalizedTime = (event.getTimestamp().toEpochMilli() / 1800000) * 1800000;
        eventId = event.getUserId() + ":" + event.getMagazineId() + ":" + normalizedTime;
        processedKey = "processed:" + eventId;

        when(redisKeyManager.getMagazineProcessedEventKey(eventId)).thenReturn(processedKey);
    }

    @ParameterizedTest(name = "isFirstProcess={0}, isException={1}")
    @CsvSource({
            "true,false",   // 최초 처리
            "false,false",  // 중복 이벤트
            "true,true"     // 예외 발생
    })
    void processEngagement_ParamTest(boolean isFirstProcess, boolean isException) {
        // given
        if (isException) {
            when(valueOps.setIfAbsent(processedKey, "1", 30, TimeUnit.MINUTES))
                    .thenThrow(new RuntimeException("Redis error"));
        } else {
            when(valueOps.setIfAbsent(processedKey, "1", 30, TimeUnit.MINUTES))
                    .thenReturn(isFirstProcess);
        }

        // when
        consumer.processEngagement(event, acknowledgment);

        // then
        verify(valueOps).setIfAbsent(processedKey, "1", 30, TimeUnit.MINUTES);

        if (isException || !isFirstProcess) {
            verify(popularityService, never()).processEngagement(anyLong(), anyLong(), anyLong(), anyDouble());
        } else {
            verify(popularityService).processEngagement(
                    eq(event.getMagazineId()),
                    eq(event.getUserId()),
                    eq(event.getDwellTime()),
                    eq(event.getScrollPercentage())
            );
        }
    }
}
