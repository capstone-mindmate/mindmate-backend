package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.dto.FilteredContentDTO;
import com.mindmate.mindmate_server.chat.dto.UserFilteringHistoryDTO;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFilteringService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyManager redisKeyManager;
    private final UserService userService;

    public UserFilteringHistoryDTO getUserFilteringHistory(Long userId) {
        // 사용자가 참여한 모든 채팅방의 필터링 카운트 키 조회 -> 24시간 이내 필터링이 걸린 채팅방임
        Set<String> filteringCountKeys = redisTemplate.keys(redisKeyManager.getFilteringCountKey(userId, "*"));
        Map<Long, Integer> roomFilterCounts = new HashMap<>();
        List<FilteredContentDTO> recentFilteredContents = new ArrayList<>();

        // 채팅방별로 필터링 횟수 및 내용 조회
        for (String key : filteringCountKeys) {
            String roomIdStr = key.substring(key.lastIndexOf(":") + 1);
            Long roomId = Long.parseLong(roomIdStr);

            Object count = redisTemplate.opsForValue().get(key);
            if (count != null) {
                roomFilterCounts.put(roomId, Integer.parseInt(count.toString()));
            }

            String contentKey = redisKeyManager.getFilteringContentKey(userId, roomId);
            List<Object> contents = redisTemplate.opsForList().range(contentKey, 0, -1);
            Set<String> uniqueContents = new LinkedHashSet<>(); // 순서 유지

            if (contents != null && !contents.isEmpty()) {
                for (Object content : contents) {
                    String contentStr = content.toString();
                    if (uniqueContents.add(contentStr)) { // 중복이 아닌 경우에만 추가
                        recentFilteredContents.add(new FilteredContentDTO(roomId, contentStr));
                    }
                }
            }
        }

        User user = userService.findUserById(userId);

        return UserFilteringHistoryDTO.builder()
                .userId(userId)
                .email(user.getEmail())
                .nickname(user.getProfile().getNickname())
                .roomFilterCounts(roomFilterCounts)
                .recentFilteredContents(recentFilteredContents)
                .totalFilterCount(roomFilterCounts.values().stream().mapToInt(Integer::intValue).sum())
                .build();
    }

    public List<UserFilteringHistoryDTO> getUserFilteringHistories() {
        Set<String> allFilteringCountKeys = redisTemplate.keys("filtering:count:*");
        Set<Long> userIds = new HashSet<>();

        for (String key : allFilteringCountKeys) {
            try {
                String[] parts = key.split(":");
                if (parts.length >= 3) {
                    userIds.add(Long.parseLong(parts[2]));
                }
            } catch (Exception e) {
                log.error("Error extracting user ID from key: {}", key);
            }
        }

        // 각 사용자의 필터링 이력 조회
        List<UserFilteringHistoryDTO> results = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                results.add(getUserFilteringHistory(userId));
            } catch (Exception e) {
                log.error("Error getting filtering history for user {}: {}", userId, e.getMessage());
            }
        }

        results.sort((a, b) -> Integer.compare(b.getTotalFilterCount(), a.getTotalFilterCount()));

        return results;
    }
}
