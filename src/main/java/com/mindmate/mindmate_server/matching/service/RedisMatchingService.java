package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisMatchingService {
    private final StringRedisTemplate redisTemplate;

    private static final String MATCHING_SET_KEY = "matching:available:%s";
    private final static String USER_ACTIVE_MATCHING_COUNT = "user:%d:activeMatchings";

    public void addMatchingToAvailableSet(Matching matching) {
        String setKey = buildKey(MATCHING_SET_KEY, matching.getCreatorRole());
        redisTemplate.opsForSet().add(setKey, matching.getId().toString());

        setExpiry(setKey, 24);
    }

    public Long getRandomMatching(InitiatorType userRole) {
        InitiatorType targetRole = (userRole == InitiatorType.SPEAKER)
                ? InitiatorType.LISTENER
                : InitiatorType.SPEAKER;

        String setKey = String.format(MATCHING_SET_KEY, targetRole);

        String matchingId = redisTemplate.opsForSet().randomMember(setKey);

        if (matchingId.isEmpty()) {
            return null; // 가능한 매칭 없음
        }

        return Long.valueOf(matchingId);
    }

    public void removeMatchingFromAvailableSet(Long matchingId, InitiatorType creatorRole) {
        String setKey = buildKey(MATCHING_SET_KEY, creatorRole);
        redisTemplate.opsForSet().remove(setKey, matchingId.toString());
    }

    // 매칭 가능한 방 수??

    public void incrementUserActiveMatchingCount(Long userId) {
        String key = buildKey(USER_ACTIVE_MATCHING_COUNT, userId);
        redisTemplate.opsForValue().increment(key);
        setExpiry(key, 24);
    }

    public void decrementUserActiveMatchingCount(Long userId) {
        String key = buildKey(USER_ACTIVE_MATCHING_COUNT, userId);
        redisTemplate.opsForValue().decrement(key);
    }

    public int getUserActiveMatchingCount(Long userId) {
        String key = buildKey(USER_ACTIVE_MATCHING_COUNT, userId);
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count.toString()) : 0;
    }

    private String buildKey(String pattern, Object... args) {
        return String.format(pattern, args);
    }

    private void setExpiry(String key, int hours) {
        redisTemplate.expire(key, hours, TimeUnit.HOURS);
    }


    public void cleanupMatchingKeys(Matching matching) {
        String setKey = buildKey(MATCHING_SET_KEY, matching.getCreatorRole());

        if (!matching.isOpen()) {
            redisTemplate.delete(setKey);
        }
    }

    // 사용자 로그아웃 or 세션 종료될 때 count 캐시 지우는 것도?

}
