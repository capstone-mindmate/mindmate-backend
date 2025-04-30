package com.mindmate.mindmate_server.magazine.util;

import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class MagazinePopularityDecayScheduler {
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyManager redisKeyManager;

    private static final double DAILY_DECAY_RATE = 0.1; // 매일 인기도 10% 감소

    @Scheduled(cron = "0 0 0 * * ?")
    public void decayPopularityScores() {
        decayScores(redisKeyManager.getMagazinePopularityKey());

        for (MatchingCategory category : MatchingCategory.values()) {
            decayScores(redisKeyManager.getCategoryPopularityKey(category.name()));
        }
    }

    private void decayScores(String key) {
        Set<ZSetOperations.TypedTuple<String>> scoresWithIds = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

        if (scoresWithIds != null) {
            for (ZSetOperations.TypedTuple<String> tuple : scoresWithIds) {
                String magazineId = tuple.getValue();
                Double currentScore = tuple.getScore();

                if (magazineId != null && currentScore != null) {
                    double newScore = currentScore * (1 - DAILY_DECAY_RATE);

                    if (newScore < 0.1) {
                        redisTemplate.opsForZSet().remove(key, magazineId);
                    } else {
                        redisTemplate.opsForZSet().add(key, magazineId, newScore);
                    }
                }
            }
        }
    }
}
