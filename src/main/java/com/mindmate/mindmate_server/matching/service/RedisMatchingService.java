package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisMatchingService {
    private final StringRedisTemplate redisTemplate;
    private final MatchingRepository matchingRepository;

    private static final String MATCHING_SET_KEY = "matching:available:%s";
    private final static String USER_ACTIVE_MATCHING_COUNT = "user:%d:activeMatchings";

    public void addMatchingToAvailableSet(Matching matching) {
        String setKey = buildKey(MATCHING_SET_KEY, matching.getCreatorRole());
        redisTemplate.opsForSet().add(setKey, matching.getId().toString());

        setExpiry(setKey, 24);
    }

//    public Long getRandomMatching(InitiatorType userRole) {
//        InitiatorType targetRole = (userRole == InitiatorType.SPEAKER)
//                ? InitiatorType.LISTENER
//                : InitiatorType.SPEAKER;
//
//        String setKey = String.format(MATCHING_SET_KEY, targetRole);
//
//        String matchingId = redisTemplate.opsForSet().randomMember(setKey);
//
//        if (matchingId.isEmpty()) {
//            return null; // 가능한 매칭 없음
//        }
//
//        return Long.valueOf(matchingId);
//    }

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

        if (!matching.isOpen()) {
            String setKey = buildKey(MATCHING_SET_KEY, matching.getCreatorRole());
            redisTemplate.opsForSet().remove(setKey, matching.getId());
        }
    }

    // 사용자 로그아웃 or 세션 종료될 때 count 캐시 지우는 것도?

    // 프로필 기반한 가중치 계산
    // 가중치 계산 기반한 매칭방 선택
    public Long getRandomMatching(User user, InitiatorType userRole) {
        InitiatorType targetRole = (userRole == InitiatorType.SPEAKER)
                ? InitiatorType.LISTENER
                : InitiatorType.SPEAKER;

        String setKey = buildKey(MATCHING_SET_KEY, targetRole);
        Set<String> matchingIds = redisTemplate.opsForSet().members(setKey);

        if (matchingIds == null || matchingIds.isEmpty()) {
            return null;
        }

        // 매칭 수 많으면 랜덤하게 선택 백개만
        List<String> candidateIds = new ArrayList<>(matchingIds);
        if (candidateIds.size() > 100) {
            Collections.shuffle(candidateIds);
            candidateIds = candidateIds.subList(0, 100);
        }

        Map<Long, Double> scoredMatches = new HashMap<>();

        for (String candidateId : candidateIds) {
            try {
                Long matchingId = Long.valueOf(candidateId);
                Matching matching =  matchingRepository.findById(matchingId).orElse(null);

                if (matching == null || !matching.isOpen() || !matching.isAllowRandom()) {
                    redisTemplate.opsForSet().remove(setKey, candidateId);
                    continue;
                }

                double score = calculateMatchingScore(user, matching);
                scoredMatches.put(matchingId, score);
            } catch (Exception e) {
                log.error("계산 오류: {}", candidateId, e);
            }
        }

        if (scoredMatches.isEmpty()) {
            return null;
        }

        List<Map.Entry<Long, Double>> topMatches = scoredMatches.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        int randomIndex = (int)(Math.random() * topMatches.size());
        return topMatches.get(randomIndex).getKey();
    }

    private double calculateMatchingScore(User user, Matching matching) {

        double score = 0.0;
        Profile userProfile = user.getProfile();

        if (userProfile != null) {
            User creator = matching.getCreator();
            Profile creatorProfile = creator.getProfile();

            if (creatorProfile != null) {
                if (!userProfile.getDepartment().isEmpty() &&
                        userProfile.getDepartment().equals(creatorProfile.getDepartment())) {
                    score += 20.0;
                }
                else if (!userProfile.getDepartment().isEmpty() &&
                        !creatorProfile.getDepartment().isEmpty() &&
                        isSameCollege(userProfile.getDepartment(), creatorProfile.getDepartment())) {
                    score += 15.0;
                }

                int yearDiff = Math.abs(userProfile.getEntranceTime() - creatorProfile.getEntranceTime());
                score += Math.max(0, 20 - yearDiff * 5);
            }
        }

        score += Math.random() * 10;

        return score;
    }

    private boolean isSameCollege(String dept1, String dept2) {
        String college1 = extractCollege(dept1);
        String college2 = extractCollege(dept2);
        return !college1.isEmpty() && college1.equals(college2);
    }

    // 학과 -> 단과대 - 이건 나중에 수정해야됨
    private String extractCollege(String department) {
        if (department.contains("공학") || department.contains("응용화학")) return "공과대학";
        if (department.contains("경영") || department.contains("경제")) return "경영대학";
        if (department.contains("소프트") || department.contains("미디어")) return "소프트웨어융합";
        if (department.contains("사회") || department.contains("정치")) return "사회과학대학";
        if (department.contains("국어") || department.contains("불어")) return "인문대학";
        return "";
    }

}
