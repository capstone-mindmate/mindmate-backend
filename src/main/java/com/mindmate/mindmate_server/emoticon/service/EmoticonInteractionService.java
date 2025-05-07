package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmoticonInteractionService {
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyManager redisKeyManager;


    @Transactional
    public void incrementViewCount(Long emoticonId, Long userId) {
        String viewKey = redisKeyManager.getEmoticonViewCountKey(emoticonId, userId);
        if (redisTemplate.opsForValue().setIfAbsent(viewKey, "1", 10, TimeUnit.MINUTES)) {
            String dailyKey = redisKeyManager.getEmoticonDailyViewKey(emoticonId);
            Long newCount = redisTemplate.opsForValue().increment(dailyKey);
            log.info("[이모티콘 조회수 증가] emoticonId={}, userId={}, dailyViewKey={}, newDailyViewCount={}",
                    emoticonId, userId, dailyKey, newCount);
        }
    }

    public void incrementUsage(Long emoticonId) {
        String usageKey = redisKeyManager.getEmoticonUsageKey(emoticonId);
        Long newCount = redisTemplate.opsForValue().increment(usageKey);
        log.info("[이모티콘 사용수 증가] emoticonId={}, usageKey={}, newUsageCount={}", emoticonId, usageKey, newCount);
    }

    public void handlePurchase(Long emoticonId) {
        String purchaseKey = redisKeyManager.getEmoticonPurchaseKey();
        Double newScore = redisTemplate.opsForZSet().incrementScore(purchaseKey, emoticonId.toString(), 1);
        log.info("[이모티콘 구매수 증가] emoticonId={}, purchaseKey={}, newPurchaseScore={}", emoticonId, purchaseKey, newScore);
    }

}
