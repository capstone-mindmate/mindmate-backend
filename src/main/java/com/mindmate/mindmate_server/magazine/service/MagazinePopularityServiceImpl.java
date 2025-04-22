package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagazinePopularityServiceImpl implements MagazinePopularityService {
    private final StringRedisTemplate redisTemplate;
    private final RedisKeyManager redisKeyManager;
    private final MagazineRepository magazineRepository;

    // 가중치 상수
    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 3.0;
    private static final double DWELL_TIME_WEIGHT = 0.8;
    private static final double SCROLL_COMPLETE_WEIGHT = 3.0;
    private static final double RECENT_WEIGHT = 2.5;
    private static final double RECENT_DECAY = 0.05;

    /**
     * 조회수 관리
     * 사용자가 매거진을 조회할 떄마다 점수 부여 -> 30분 이내 중복 조회 방지
     */
    @Override
    public void incrementViewCount(Magazine magazine, Long userId, String ipAddress) {
        try {
            String viewKey = redisKeyManager.getMagazineViewedKey(userId, magazine.getId());
            Boolean isFirstView = redisTemplate.opsForValue().setIfAbsent(viewKey, "1", 30, TimeUnit.MINUTES);

            if (Boolean.TRUE.equals(isFirstView)) {
                String viewCountKey = redisKeyManager.getMagazineViewCountKey(magazine.getId());
                redisTemplate.opsForValue().increment(viewCountKey);

                double score = calculateScoreWithRecency(VIEW_WEIGHT, magazine.getCreatedAt());
                updatePopularityScore(magazine.getId(), score, magazine.getCategory());

                log.debug("매거진 조회수 증가: magazineId={}, userId={}, score={}", magazine.getId(), userId, score);
            }
        } catch (Exception e) {
            log.error("조수 증가 처리 중 오류 발생: magazineId{}, userid={}", magazine.getId(), userId, e);
        }
    }


    /**
     * 좋아요 toggle에 따른 점수 부여
     */
    @Override
    public void updateLikeScore(Magazine magazine, boolean isLiked) {
        double baseScore = isLiked ? LIKE_WEIGHT : -LIKE_WEIGHT;
        double score = calculateScoreWithRecency(baseScore, magazine.getCreatedAt());
        updatePopularityScore(magazine.getId(), score, magazine.getCategory());
    }

    /**
     * 체류 시간 + 스크롤 데이터 처리
     * - 매거진에 머무는 시간에 따라 점수 부여 -> 분당 0.5점 최대 5분
     * - 매거진 80% 이상의 스크롤 시 점수 부여
     */
    @Override
    public void processEngagement(Long magazineId, Long userId, Long dwellTime, Double scrollPercentage) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new CustomException(MagazineErrorCode.MAGAZINE_NOT_FOUND));

        double totalScore = 0;
        if (dwellTime != null && dwellTime > 0) {
            String dwellTimeKey = redisKeyManager.getMagazineDwellTimeKey(magazineId);
            redisTemplate.opsForZSet().add(dwellTimeKey, userId.toString(), dwellTime);

            double dwellTimeMinutes = dwellTime / (1000.0 * 60);
            double dwellScore = Math.min(dwellTimeMinutes, 5.0) * DWELL_TIME_WEIGHT;
            totalScore += dwellScore;
        }

        if (scrollPercentage != null && scrollPercentage > 0) {
            totalScore = (scrollPercentage / 100.0) * SCROLL_COMPLETE_WEIGHT;
        }

        if (totalScore > 0) {
            double finalScore = calculateScoreWithRecency(totalScore, magazine.getCreatedAt());
            updatePopularityScore(magazineId, finalScore, magazine.getCategory());
        }
    }


    /**
     * 새로 발행된 메거진에 점수 부여
     * -> 최신 매거진을 인기 매거진으로 선정할 확률 높여주기
     */
    @Override
    public void initializePopularityScore(Magazine magazine) {
        double initialScore = calculateScoreWithRecency(RECENT_WEIGHT, magazine.getCreatedAt());
        updatePopularityScore(magazine.getId(), initialScore, magazine.getCategory());
        log.info("매거진 초기 인기도 점수 설정: magazineId={}, score={}", magazine.getId(), initialScore);
    }


    @Override
    public void removePopularityScores(Long magazineId, MatchingCategory category) {
        String magazineIdString = magazineId.toString();
        redisTemplate.opsForZSet().remove(redisKeyManager.getMagazinePopularityKey(), magazineIdString);

        if (category != null) {
            redisTemplate.opsForZSet().remove(redisKeyManager.getCategoryPopularityKey(category.name()), magazineIdString);
        }
    }


    /**
     * 매거진 인기도 값 기준 정렬
     */
    @Override
    public List<MagazineResponse> getPopularMagazines(int limit) {
        String popularity = redisKeyManager.getMagazinePopularityKey();
        Set<String> topMagazineIds = redisTemplate.opsForZSet().reverseRange(popularity, 0, limit - 1);

        // Redis에 데이터가 없는 경우 db의 좋아요 기준 조회
        if (topMagazineIds == null || topMagazineIds.isEmpty()) {
            return magazineRepository.findTop10ByMagazineStatusOrderByLikeCountDesc(MagazineStatus.PUBLISHED)
                    .stream()
                    .map(MagazineResponse::from)
                    .collect(Collectors.toList());
        }

        return getMagazineResponsesFromIds(topMagazineIds);
    }

    /**
     * 해당 카테고리의 매거진들 중 인기도 값 정렬
     */
    @Override
    public List<MagazineResponse> getPopularMagazinesByCategory(String category, int limit) {
        String categoryKey = redisKeyManager.getCategoryPopularityKey(category);
        Set<String> topMagazineIds = redisTemplate.opsForZSet().reverseRange(categoryKey, 0, limit - 1);

        if (topMagazineIds == null || topMagazineIds.isEmpty()) {
            try {
                return magazineRepository.findByMagazineStatusAndCategoryOrderByLikeCountDesc(
                                MagazineStatus.PUBLISHED, MatchingCategory.valueOf(category))
                        .stream()
                        .map(MagazineResponse::from)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.error("유효하지 않은 카테고리: {}", category, e);
                return Collections.emptyList();
            }
        }

        return getMagazineResponsesFromIds(topMagazineIds);
    }


    /**
     * 매거진 최신성 고려
     */
    private double calculateScoreWithRecency(double baseScore, LocalDateTime createdAt) {
        long daysOld = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        double recencyFactor = Math.exp(-RECENT_DECAY * daysOld);

        if (daysOld <= 3) {
            recencyFactor += 1.5;
        }

        return baseScore * recencyFactor;
    }

    /**
     * 매거진 ID 목록으로부터 매거진 응답 객체 목록 생성
     */
    private List<MagazineResponse> getMagazineResponsesFromIds(Set<String> magazineIdStrings) {
        List<Long> magazineIds = magazineIdStrings.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Magazine> magazines = magazineRepository.findAllById(magazineIds);
        Map<Long, Magazine> magazineMap = magazines.stream()
                .collect(Collectors.toMap(Magazine::getId, Function.identity()));

        return magazineIds.stream()
                .map(magazineMap::get)
                .filter(Objects::nonNull)
                .filter(m -> m.getMagazineStatus() == MagazineStatus.PUBLISHED)
                .map(MagazineResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 인기도 점수 업데이트
     */
    private void updatePopularityScore(Long magazineId, double score, MatchingCategory category) {
        String popularityKey = redisKeyManager.getMagazinePopularityKey();
        redisTemplate.opsForZSet().incrementScore(popularityKey, magazineId.toString(), score);

        if (category != null) {
            String categoryKey = redisKeyManager.getCategoryPopularityKey(category.name());
            redisTemplate.opsForZSet().incrementScore(categoryKey, magazineId.toString(), score);
        }
    }
}
