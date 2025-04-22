package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.magazine.dto.MagazineEngagementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagazineEngagementConsumer {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyManager keyManager;

    @KafkaListener(
            topics = "magazine-engagement-topic",
            containerFactory = "magazineEngagementListenerContainerFactory"
    )
    public void processEngagement(MagazineEngagementEvent event) {
        try {
            // 체류 시간 처리
            if (event.getDwellTime() != null && event.getDwellTime() > 0) {
                String dwellTimeKey = keyManager.getMagazineDwellTimeKey(event.getMagazineId());
                redisTemplate.opsForZSet().add(dwellTimeKey, event.getUserId().toString(), event.getDwellTime());

                double dwellTimeMinutes = event.getDwellTime() / (1000.0 * 60);
                double weight = Math.min(dwellTimeMinutes, 5.0) * 0.5; // 최대 5분

                String popularityKey = keyManager.getMagazinePopularityKey();
                redisTemplate.opsForZSet().incrementScore(popularityKey, event.getMagazineId().toString(), weight);
            }

            // 스크롤 처리
            if (event.getScrollPercentage() != null && event.getScrollPercentage() > 0) {
                if (event.getScrollPercentage() >= 80) {
                    String popularityKey = keyManager.getMagazinePopularityKey();
                    redisTemplate.opsForZSet().incrementScore(popularityKey, event.getMagazineId().toString(), 2.0);
                }
            }
            log.info("매거진 참여 이벤트 처리 완료: magazineId={}", event.getMagazineId());
        } catch (Exception e) {
            log.error("매거진 참여 이벤트 처리 중 오류 발생", e);
        }
    }
}
