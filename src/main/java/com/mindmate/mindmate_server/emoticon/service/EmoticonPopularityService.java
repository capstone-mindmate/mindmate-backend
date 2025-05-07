package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonCount;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmoticonPopularityService {
    private final EmoticonRepository emoticonRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyManager redisKeyManager;

    private static final double VIEW_WEIGHT = 0.25;
    private static final double USAGE_WEIGHT = 0.30;
    private static final double PURCHASE_WEIGHT = 0.35;
    private static final double RECENCY_WEIGHT = 0.10;
    private static final double RECENCY_DECAY = 0.02;

    public List<EmoticonResponse> getMostPurchasedEmoticons(int limit) {
        String key = redisKeyManager.getEmoticonPopularityKey();
        Set<ZSetOperations.TypedTuple<String>> top = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, limit - 1);
        return getEmoticonResponsesFromRedisSet(top);
    }

    public List<EmoticonResponse> getMostViewedEmoticons(int limit) {
        List<EmoticonCount> viewCounts = new ArrayList<>();
        for (Emoticon emoticon : emoticonRepository.findAll()) {
            String key = redisKeyManager.getEmoticonDailyViewKey(emoticon.getId());
            Long count = Optional.ofNullable(redisTemplate.opsForValue().get(key))
                    .map(Long::parseLong).orElse(0L);
            viewCounts.add(new EmoticonCount(emoticon, count));
        }
        return viewCounts.stream()
                .sorted(Comparator.comparingLong(EmoticonCount::getCount).reversed())
                .limit(limit)
                .map(vc -> EmoticonResponse.from(vc.getEmoticon(), false))
                .collect(Collectors.toList());
    }

    public List<EmoticonResponse> getMostUsedEmoticons(int limit) {
        List<EmoticonCount> usageCounts = new ArrayList<>();
        for (Emoticon emoticon : emoticonRepository.findAll()) {
            String key = redisKeyManager.getEmoticonPurchaseKey();
            Long count = Optional.ofNullable(redisTemplate.opsForValue().get(key))
                    .map(Long::parseLong).orElse(0L);
            usageCounts.add(new EmoticonCount(emoticon, count));
        }
        return usageCounts.stream()
                .sorted(Comparator.comparingLong(EmoticonCount::getCount).reversed())
                .limit(limit)
                .map(uc -> EmoticonResponse.from(uc.getEmoticon(), false))
                .collect(Collectors.toList());
    }

    public List<EmoticonResponse> getOverallPopularEmoticons(int limit) {
        String key = redisKeyManager.getEmoticonPopularityKey();
        Set<ZSetOperations.TypedTuple<String>> top = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, limit - 1);
        return getEmoticonResponsesFromRedisSet(top);
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void calculatePopularity() {
        emoticonRepository.findAll().forEach(emoticon -> {
            double totalScore = calculateScore(emoticon);
            redisTemplate.opsForZSet().add(
                    redisKeyManager.getEmoticonPopularityKey(),
                    emoticon.getId().toString(),
                    totalScore
            );
        });
    }

    private double calculateScore(Emoticon emoticon) {
        Long views = getDailyViews(emoticon.getId());
        Long usage = getUsageCount(emoticon.getId());
        Long purchases = getWeeklyPurchases(emoticon.getId());

        double recencyScore = calculateRecency(emoticon.getCreatedAt());

        double totalScore = (views * VIEW_WEIGHT)
                + (usage * USAGE_WEIGHT)
                + (purchases * PURCHASE_WEIGHT)
                + (recencyScore * RECENCY_WEIGHT);

        log.debug("[이모티콘 점수 계산] emoticonId={}, views={}, usage={}, purchases={}, recencyScore={}, totalScore={}",
                emoticon.getId(), views, usage, purchases, recencyScore, totalScore);

        return totalScore;
    }

    private Long getUsageCount(Long emoticonId) {
        String key = redisKeyManager.getEmoticonUsageKey(emoticonId);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(Long::parseLong).orElse(0L);
    }

    private Long getDailyViews(Long emoticonId) {
        String key = redisKeyManager.getEmoticonDailyViewKey(emoticonId);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(Long::parseLong).orElse(0L);
    }

    private Long getWeeklyPurchases(Long emoticonId) {
        String key = redisKeyManager.getEmoticonPurchaseKey();
        return Optional.ofNullable(redisTemplate.opsForZSet().score(key, emoticonId.toString()))
                .map(Double::longValue).orElse(0L);
    }

    private double calculateRecency(LocalDateTime createdAt) {
        long daysOld = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        return Math.max(0, 1 - (daysOld * RECENCY_DECAY));
    }

    private List<EmoticonResponse> getEmoticonResponsesFromRedisSet(Set<ZSetOperations.TypedTuple<String>> set) {
        if (set == null) return Collections.emptyList();
        List<Long> ids = set.stream()
                .map(t -> Long.parseLong(t.getValue()))
                .collect(Collectors.toList());
        Map<Long, Emoticon> emoticonMap = emoticonRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Emoticon::getId, e -> e));
        return ids.stream()
                .map(emoticonMap::get)
                .filter(Objects::nonNull)
                .map(e -> EmoticonResponse.from(e, false))
                .collect(Collectors.toList());
    }


}
