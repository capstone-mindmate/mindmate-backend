package com.mindmate.mindmate_server.review.repository;

import com.mindmate.mindmate_server.review.dto.ProfileReviewSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class ReviewRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REVIEW_SUMMARY_KEY_PREFIX = "review:summary:profile:";
    private static final String TAG_COUNTS_KEY_PREFIX = "review:tagCounts:profile:";

    private static final long CACHE_TTL_HOURS = 24;


    // 고민 : 자주 조회되는 프로필만 캐싱?
    public void saveReviewSummary(Long profileId, ProfileReviewSummaryResponse summary) {
        String key = REVIEW_SUMMARY_KEY_PREFIX + profileId;
        redisTemplate.opsForValue().set(key, summary, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public ProfileReviewSummaryResponse getReviewSummary(Long profileId) {
        String key = REVIEW_SUMMARY_KEY_PREFIX + profileId;
        return (ProfileReviewSummaryResponse) redisTemplate.opsForValue().get(key);
    }

    public void saveTagCounts(Long profileId, Map<String, Integer> tagCounts) {
        String key = TAG_COUNTS_KEY_PREFIX + profileId;
        redisTemplate.opsForHash().putAll(key, tagCounts);
        redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public Map<String, Integer> getTagCounts(Long profileId) {
        String key = TAG_COUNTS_KEY_PREFIX + profileId;
        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(key);

        if (rawMap == null || rawMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            String tagName = entry.getKey().toString();
            Integer count = null;

            Object value = entry.getValue();
            if (value instanceof Integer) {
                count = (Integer) value;
            } else if (value instanceof Long) {
                count = ((Long) value).intValue();
            } else if (value instanceof String) {
                count = Integer.parseInt(value.toString());
            } else if (value instanceof Number) {
                count = ((Number) value).intValue();
            } else { // 다르면 스킵
                continue;
            }

            result.put(tagName, count);
        }

        return result;
    }

    public void incrementTagCount(Long profileId, String tagContent) {
        String key = TAG_COUNTS_KEY_PREFIX + profileId;
        redisTemplate.opsForHash().increment(key, tagContent, 1);
        redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public void deleteReviewSummaryCache(Long profileId) {
        String key = REVIEW_SUMMARY_KEY_PREFIX + profileId;
        redisTemplate.delete(key);
    }
}
