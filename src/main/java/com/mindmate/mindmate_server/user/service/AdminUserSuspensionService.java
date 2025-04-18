package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.report.dto.UnsuspendRequest;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.SuspendedUserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserSuspensionService {
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyManager redisKeyManager;
    private final SlackNotifier slackNotifier;

    /**
     * 사용자 정지 처리
     */
    @Transactional
    public void suspendUser(Long userId, int reportCount, Duration duration, String reason) {
        User user = userService.findUserById(userId);

        if (user.getCurrentRole().equals(RoleType.ROLE_ADMIN)) {
            throw new CustomException(UserErrorCode.ADMIN_SUSPENSION_NOT_ALLOWED);
        }

        if (reportCount >= 0) {
            user.setReportCount(reportCount);
        }

        user.suspend(duration);
        userService.save(user);

        String suspensionKey = redisKeyManager.getUserSuspensionKey(userId);
        redisTemplate.opsForValue().set(suspensionKey, "suspended");

        if (duration.toDays() > 0) {
            redisTemplate.expire(suspensionKey, duration.toDays(), TimeUnit.DAYS);
        } else {
            redisTemplate.expire(suspensionKey, duration.toHours(), TimeUnit.HOURS);
        }

        slackNotifier.sendSuspensionAlert(user, reason, duration);
    }

    /**
     * 사용자 정지 해제 처리
     */
    @Transactional
    public void unsuspendUser(Long userId, UnsuspendRequest request) {
        User user = userService.findUserById(userId);
        Integer reportCount = request.getReportCount();

        if (!user.getCurrentRole().equals(RoleType.ROLE_SUSPENDED)) {
            throw new CustomException(UserErrorCode.USER_ALREADY_NOT_SUSPENDED);
        }

        if (reportCount != null) {
            user.setReportCount(reportCount);
        }

        user.unsuspend();
        userService.save(user);

        String suspensionKey = redisKeyManager.getUserSuspensionKey(userId);
        redisTemplate.delete(suspensionKey);
    }

    /**
     * 정지된 모든 사용자 조회
     */
    public List<SuspendedUserDTO> getAllSuspendedUsers() {
        Set<String> suspensionKeys = redisTemplate.keys("user:suspension:*");
        List<SuspendedUserDTO> suspendedUsers = new ArrayList<>();

        for (String key : suspensionKeys) {
            try {
                String userId = key.substring("user:suspension:".length());
                User user = userService.findUserById(Long.parseLong(userId));

                String suspensionReason = "Unknown";
                Object reasonValue = redisTemplate.opsForValue().get(key);
                if (reasonValue != null) {
                    suspensionReason = reasonValue.toString();
                }

                SuspendedUserDTO dto = SuspendedUserDTO.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getProfile().getNickname())
                        .reportCount(user.getReportCount())
                        .suspensionEndTime(user.getSuspensionEndTime())
                        .suspensionReason(suspensionReason)
                        .build();
                suspendedUsers.add(dto);
            } catch (Exception e) {
                log.error("Error processing suspension key {}: {}", key, e.getMessage());
            }
        }
        return suspendedUsers;
    }
}
