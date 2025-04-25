package com.mindmate.mindmate_server.magazine.util;

import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class MagazineDataCleanUpScheduler {
    private final MagazineService magazineService;

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyManager redisKeyManager;

    private static final double POPULARITY_THRESHOLD = 0.5;
    private static final int DATA_RETENTION_DAYS = 7;

    /**
     * 필요 없는 매거진 인기도 정리
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanUpOldData() {
        cleanupOldDwellTimeData();

        cleanupLowPopularityData();
    }

    private void cleanupOldDwellTimeData() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DATA_RETENTION_DAYS);
        Set<String> keys = redisTemplate.keys("magazine:dwell:*");

        if (keys != null) {
            for (String key : keys) {
                String magazineIdStr = key.substring("magazine:dwell:".length());
                try {
                    Long magazineId = Long.parseLong(magazineIdStr);
                    Magazine magazine = magazineService.findMagazineById(magazineId);

                    LocalDateTime createdAt = magazine.getCreatedAt();

                    if (createdAt.isBefore(cutoffDate)) {
                        redisTemplate.delete(key);
                    }
                } catch (NumberFormatException e) {
                    log.warn("매거진 ID 파싱 오류 : {}", magazineIdStr);
                }
            }
        }
    }

    private void cleanupLowPopularityData() {
        String popularityKey = redisKeyManager.getMagazinePopularityKey();
        Set<ZSetOperations.TypedTuple<String>> lowScoreTuples = redisTemplate.opsForZSet().rangeByScoreWithScores(popularityKey, 0, POPULARITY_THRESHOLD);

        if (lowScoreTuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : lowScoreTuples) {
                String magazineId = tuple.getValue();
                if (magazineId != null) {
                    redisTemplate.opsForZSet().remove(popularityKey, magazineId);

                    try {
                        Long id = Long.parseLong(magazineId);
                        Magazine magazine = magazineService.findMagazineById(id);
                        MatchingCategory category = magazine.getCategory();

                        if (category != null) {
                            String categoryKey = redisKeyManager.getCategoryPopularityKey(category.name());
                            redisTemplate.opsForZSet().remove(categoryKey, magazineId);
                        }
                    } catch (Exception e) {
                        log.warn("매거진 카테고리 인기도 정리 중 오류: magazineId={}", magazineId, e);
                    }
                }
            }
        }
    }
}
