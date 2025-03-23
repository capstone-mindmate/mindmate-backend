package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisMatchingService {
    private final StringRedisTemplate redisTemplate;

    private static final String MATCHING_SET_KEY = "matching:available:%s";

    public void addMatchingToAvailableSet(Matching matching) {
        String setKey = String.format(MATCHING_SET_KEY, matching.getCreatorRole());
        redisTemplate.opsForSet().add(setKey, matching.getId().toString());
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
        String setKey = String.format(MATCHING_SET_KEY, creatorRole);
        redisTemplate.opsForSet().remove(setKey, matchingId.toString());
    }

}
