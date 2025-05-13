package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.service.AdminUserSuspensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilteringEventConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyManager redisKeyManager;
    private final AdminUserSuspensionService suspensionService;

    private static final int CHAT_FILTERING_SUSPENSION_THRESHOLD = 5; // 필터링 정지 횟수
    private static final int CHAT_FILTERING_SUSPENSION_TIME = 2; // 정지 시간
    private static final int CHAT_FILTERING_EXPIRY_HOURS = 24; // 해당 레디스 값 유효 시간

    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "filtering-event-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void processFilteringEvent(ConsumerRecord<String, ChatMessageEvent> record, Acknowledgment ack) {
        ChatMessageEvent event = record.value();

        if (!event.isFiltered() || event.getSenderId() == null) {
            ack.acknowledge();
            return;
        }

        try {
            String filteringCountKey = redisKeyManager.getFilteringCountKey(event.getSenderId(), event.getRoomId());
            String filteringContentKey = redisKeyManager.getFilteringContentKey(event.getSenderId(), event.getRoomId());

            Long count = redisTemplate.opsForValue().increment(filteringCountKey, 1);

            // 첫 필터링 발생 -> count 값 24시간 만료 설정
            if (count != null && count == 1) {
                redisTemplate.expire(filteringCountKey, CHAT_FILTERING_EXPIRY_HOURS, TimeUnit.HOURS);
            }

            String truncatedContent = event.getContent().length() > 100 ? event.getContent().substring(0, 100) + "..." : event.getContent();
            redisTemplate.opsForList().leftPush(filteringContentKey, truncatedContent);
            redisTemplate.opsForList().trim(filteringContentKey, 0, 4);
            redisTemplate.expire(filteringContentKey, CHAT_FILTERING_EXPIRY_HOURS, TimeUnit.HOURS);

            if (count != null && count >= CHAT_FILTERING_SUSPENSION_THRESHOLD) {
//            redisTemplate.expire(filteringContentKey, )
                applySuspension(event.getSenderId());

                redisTemplate.delete(filteringCountKey);
                log.info("User {} reached filtering threshold in room {}. Applied suspension.",
                        event.getSenderId(), event.getRoomId());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing filtering event: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }

    private void applySuspension(Long userId) {
        suspensionService.suspendUser(
                userId,
                -1,
                Duration.ofHours(CHAT_FILTERING_SUSPENSION_TIME),
                "채팅 필터링 위반 (5회 이상)");
    }
}
