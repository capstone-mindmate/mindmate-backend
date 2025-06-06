package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.util.KafkaStandardRetry;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.magazine.dto.MagazineEngagementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagazineEngagementConsumer {
    private final MagazinePopularityService popularityService;
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyManager redisKeyManager;

//    @KafkaStandardRetry
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000,
                    random = true
            ),
            dltTopicSuffix = "-magazine-engagement-group-dlt",
            retryTopicSuffix = "-magazine-engagement-group-retry"
    )
    @KafkaListener(
            topics = "magazine-engagement-topic",
            groupId = "magazine-engagement-group",
            containerFactory = "magazineEngagementListenerContainerFactory"
    )
    public void processEngagement(MagazineEngagementEvent event, Acknowledgment ack) {
        try {
            long normalizedTime = (event.getTimestamp().toEpochMilli() / 1800000) * 1800000;
            String eventId = event.getUserId() + ":" + event.getMagazineId() + ":" + normalizedTime;
            String processedKey = redisKeyManager.getMagazineProcessedEventKey(eventId);

            Boolean isFirstProcess = redisTemplate.opsForValue().setIfAbsent(processedKey, "1", 30, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(isFirstProcess)) {
                log.info("매거진 이벤트 30분 이내 중복 이벤트 무시: {}", event);
                return;
            }

            popularityService.processEngagement(
                    event.getMagazineId(),
                    event.getUserId(),
                    event.getDwellTime(),
                    event.getScrollPercentage()
            );

            log.info("매거진 참여 이벤트 처리 완료: magazineId={}", event.getMagazineId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("매거진 참여 이벤트 처리 중 오류 발생", e);
        }
    }
}
