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

    private static final String MATCHING_AVAILABLE_KEY = "matching:available:%s";
    private static final String USER_ACTIVE_MATCHING_COUNT_KEY = "user:%d:activeMatchings";
    private static final int DEFAULT_EXPIRY_HOURS = 24;

    private static final Map<String, String> DEPARTMENT_TO_COLLEGE_MAP = new HashMap<>();
    static {
        DEPARTMENT_TO_COLLEGE_MAP.put("기계공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("환경안전공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("산업공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("건설시스템공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("화학공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("교통시스템공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("첨단신소재공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("건축", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("응용화학생명공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("융합시스템공학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("응용화학과", "공과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("전자공학과", "첨단ICT융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("지능형반도체공학과", "첨단ICT융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("미래모빌리티공학과", "첨단ICT융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("디지털미디어학과", "소프트웨어융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("소프트웨어학과", "소프트웨어융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("국방디지털융합학과", "소프트웨어융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("사이버보안학과", "소프트웨어융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("인공지능융합학과", "소프트웨어융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("수학과", "자연과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("화학과", "자연과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("물리학과", "자연과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("생명과학과", "자연과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("프런티어과학학부", "자연과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("경영학과", "경영대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("금융공학과", "경영대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("경영인텔리전스학과", "경영대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("글로벌경영학과", "경영대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("사회학과", "사회과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("정치외교학과", "사회과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("경제학과", "사회과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("행정학과", "사회과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("심리학과", "사회과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("스포츠레저학과", "사회과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("경제정치사회융합학부", "사회과학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("국어국문학과", "인문대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("불어불문학과", "인문대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("사학과", "인문대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("영어영문학과", "인문대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("문화콘텐츠학과", "인문대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("의학과", "의과대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("간호학과", "간호대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("약학과", "약학대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("첨단바이오융합대학", "첨단바이오융합대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("다산학부대학", "다산학부대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("자유전공학부", "다산학부대학");
        DEPARTMENT_TO_COLLEGE_MAP.put("국제학부", "인문대학");
    }


    public void addMatchingToAvailableSet(Matching matching) {
        String setKey = getAvailableMatchingSetKey(matching.getCreatorRole());
        redisTemplate.opsForSet().add(setKey, matching.getId().toString());
        setExpiry(setKey, DEFAULT_EXPIRY_HOURS);
    }

    public void removeMatchingFromAvailableSet(Long matchingId, InitiatorType creatorRole) {
        String setKey = getAvailableMatchingSetKey(creatorRole);
        redisTemplate.opsForSet().remove(setKey, matchingId.toString());
    }

    public void incrementUserActiveMatchingCount(Long userId) {
        String key = getUserActiveMatchingCountKey(userId);
        redisTemplate.opsForValue().increment(key);
        setExpiry(key, DEFAULT_EXPIRY_HOURS);
    }

    public void decrementUserActiveMatchingCount(Long userId) {
        String key = getUserActiveMatchingCountKey(userId);
        Long currentCount = redisTemplate.opsForValue().decrement(key);

        if (currentCount != null && currentCount <= 0) {
            redisTemplate.delete(key);
        }
    }

    public int getUserActiveMatchingCount(Long userId) {
        String key = getUserActiveMatchingCountKey(userId);
        String countStr = redisTemplate.opsForValue().get(key);
        return countStr != null ? Integer.parseInt(countStr) : 0;
    }

    private String buildKey(String pattern, Object... args) {
        return String.format(pattern, args);
    }

    public void cleanupMatchingKeys(Matching matching) {

        if (!matching.isOpen()) {
            String setKey = getAvailableMatchingSetKey(matching.getCreatorRole());
            redisTemplate.opsForSet().remove(setKey, matching.getId().toString());
        }
    }

    public Long getRandomMatching(User user, InitiatorType userRole) {
        InitiatorType targetRole = getOppositeRole(userRole);
        String setKey = getAvailableMatchingSetKey(targetRole);

        Set<String> matchingIds = redisTemplate.opsForSet().members(setKey);
        if (matchingIds == null || matchingIds.isEmpty()) {
            log.debug("사용 가능한 매칭 없음: userRole={}", userRole);
            return null;
        }

        List<String> candidateIds = limitCandidates(matchingIds, 100);

        Map<Long, Double> scoredMatches = calculateMatchingScores(user, candidateIds, setKey);
        if (scoredMatches.isEmpty()) {
            log.debug("유효한 매칭 없음: userId={}", user.getId());
            return null;
        }

        return selectRandomTopMatch(scoredMatches, 5);
    }

    private double calculateMatchingScore(User user, Matching matching) {
        double score = 0.0;
        Profile userProfile = user.getProfile();

        if (userProfile == null || matching.getCreator() == null || matching.getCreator().getProfile() == null) {
            return 5.0 + (Math.random() * 10);
        }

        Profile creatorProfile = matching.getCreator().getProfile();

        score += calculateDepartmentScore(userProfile, creatorProfile);

        score += calculateEntranceYearScore(userProfile, creatorProfile);

        score += Math.random() * 10;

        return score;
    }

    private double calculateDepartmentScore(Profile userProfile, Profile creatorProfile) {
        String userDept = userProfile.getDepartment();
        String creatorDept = creatorProfile.getDepartment();

        if (userDept.isEmpty() || creatorDept.isEmpty()) {
            return 0.0;
        }

        if (userDept.equals(creatorDept)) {
            return 20.0;
        }

        if (isSameCollege(userDept, creatorDept)) {
            return 15.0;
        }

        return 0.0;
    }

    private double calculateEntranceYearScore(Profile userProfile, Profile creatorProfile) {
        int yearDiff = Math.abs(userProfile.getEntranceTime() - creatorProfile.getEntranceTime());
        return Math.max(0, 20 - yearDiff * 5);
    }

    private boolean isSameCollege(String dept1, String dept2) {
        String college1 = extractCollege(dept1);
        String college2 = extractCollege(dept2);
        return !college1.isEmpty() && college1.equals(college2);
    }

    private String extractCollege(String department) {
        return DEPARTMENT_TO_COLLEGE_MAP.entrySet().stream()
                .filter(entry -> department.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }

    private List<String> limitCandidates(Set<String> matchingIds, int limit) {
        List<String> candidateIds = new ArrayList<>(matchingIds);
        if (candidateIds.size() > limit) {
            Collections.shuffle(candidateIds);
            return candidateIds.subList(0, limit);
        }
        return candidateIds;
    }

    private Map<Long, Double> calculateMatchingScores(User user, List<String> candidateIds, String setKey) {
        Map<Long, Double> scoredMatches = new HashMap<>();

        for (String candidateId : candidateIds) {
            try {
                Long matchingId = Long.valueOf(candidateId);
                Matching matching = matchingRepository.findById(matchingId).orElse(null);

                if (matching == null || !matching.isOpen() || !matching.isAllowRandom()) {
                    redisTemplate.opsForSet().remove(setKey, candidateId);
                    continue;
                }

                double score = calculateMatchingScore(user, matching);
                scoredMatches.put(matchingId, score);
            } catch (Exception e) {
                log.error("매칭 점수 계산 오류: matchingId={}", candidateId, e);
            }
        }

        return scoredMatches;
    }

    private Long selectRandomTopMatch(Map<Long, Double> scoredMatches, int topCount) {
        List<Map.Entry<Long, Double>> topMatches = scoredMatches.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topCount)
                .collect(Collectors.toList());

        int randomIndex = (int)(Math.random() * topMatches.size());
        return topMatches.get(randomIndex).getKey();
    }

    private InitiatorType getOppositeRole(InitiatorType role) {
        return (role == InitiatorType.SPEAKER) ? InitiatorType.LISTENER : InitiatorType.SPEAKER;
    }

    private String getAvailableMatchingSetKey(InitiatorType role) {
        return String.format(MATCHING_AVAILABLE_KEY, role);
    }

    private String getUserActiveMatchingCountKey(Long userId) {
        return String.format(USER_ACTIVE_MATCHING_COUNT_KEY, userId);
    }

    private void setExpiry(String key, int hours) {
        redisTemplate.expire(key, hours, TimeUnit.HOURS);
    }

}
