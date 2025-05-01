package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatEventType;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatPresenceService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisKeyManager redisKeyManager;
    private final ChatEventPublisher eventPublisher;

    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;

    public void updateUserStatus(Long userId, boolean isOnline, Long activeRoomId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
        Map<String, Object> status = new HashMap<>();
        status.put("online", isOnline);
        status.put("activeRoomId", activeRoomId);
        status.put("lastActive", LocalDateTime.now().toString());
        status.put("status", isOnline ? "ONLINE" : "OFFLINE");

        redisTemplate.opsForHash().putAll(statusKey, status);

        if (isOnline) {
            redisTemplate.expire(statusKey, 5, TimeUnit.MINUTES);
        } else {
            redisTemplate.expire(statusKey, 30, TimeUnit.MINUTES);
        }

        eventPublisher.publishUserEvent(userId, ChatEventType.USER_STATUS, status);
    }

    public Long getActiveRoom(Long userId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
        return (Long) redisTemplate.opsForHash().get(statusKey, "activeRoomId");
    }

    public boolean isUserActiveInRoom(Long userId, Long roomId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
        Boolean isOnline = (Boolean) redisTemplate.opsForHash().get(statusKey, "online");
        Object activeRoomObj = redisTemplate.opsForHash().get(statusKey, "activeRoomId");

        if (!Boolean.TRUE.equals(isOnline) || activeRoomObj == null) {
            return false;
        }

        Long activeRoomId;
        try {
            if (activeRoomObj instanceof Integer) {
                activeRoomId = ((Integer) activeRoomObj).longValue();
            } else if (activeRoomObj instanceof Long) {
                activeRoomId = (Long) activeRoomObj;
            } else if (activeRoomObj instanceof String) {
                activeRoomId = Long.parseLong((String) activeRoomObj);
            } else {
                log.warn("Unexpected activeRoomId type: {}", activeRoomObj.getClass().getName());
                return false;
            }
            return activeRoomId.equals(roomId);
        } catch (Exception e) {
            log.error("Error converting activeRoomId: {}", e.getMessage());
            return false;
        }
    }

    public Long incrementUnreadCountInRedis(Long roomId, Long userId) {
        // Redis 업데이트
        String unreadKey = redisKeyManager.getUnreadCountKey(roomId, userId);
        Long count = redisTemplate.opsForValue().increment(unreadKey);

        // WebSocket 알림
        notifyUnreadCount(roomId, userId, count);
        notifyTotalUnreadCount(userId);

        log.info("Incremented unread count in Redis for user {} in room {} to {}", userId, roomId, count);
        return count;
    }

    public void resetUnreadCount(Long roomId, Long userId) {
        String unreadKey = redisKeyManager.getUnreadCountKey(roomId, userId);
        redisTemplate.delete(unreadKey);

        notifyUnreadCount(roomId, userId, 0L);
        notifyTotalUnreadCount(userId);
    }

    public Long getTotalUnreadCount(Long userId) {
        List<ChatRoom> userChatRooms = chatRoomRepository.findActiveChatRoomByUserId(userId);
        if (userChatRooms.isEmpty()) {
            return 0L;
        }

        long totalCount = 0;

        for (ChatRoom chatRoom : userChatRooms) {
            String unreadKey = redisKeyManager.getUnreadCountKey(chatRoom.getId(), userId);
            Object redisUnreadCount = redisTemplate.opsForValue().get(unreadKey);

            if (redisUnreadCount != null) {
                // reids에 해당 채팅방의 읽지 않은 값 존재
                if (redisUnreadCount instanceof Long) {
                    totalCount += (Long) redisUnreadCount;
                } else if (redisUnreadCount instanceof Integer) {
                    totalCount += ((Integer) redisUnreadCount).longValue();
                } else if (redisUnreadCount instanceof String) {
                    try {
                        totalCount += Long.parseLong((String) redisUnreadCount);
                    } catch (NumberFormatException e) {
                        log.error("Error parsing unread count from Redis: {}", e.getMessage());
                    }
                }
            } else {
                // redis에 값이 없는 경우 db에서 읽어 오기
                Long dbUnreadCount = getUnreadCountFromDB(chatRoom, userId);
                totalCount += dbUnreadCount;

                redisTemplate.opsForValue().set(unreadKey, dbUnreadCount);
            }
        }
        return totalCount;
    }

    private Long getUnreadCountFromDB(ChatRoom chatRoom, Long userId) {
        User user = userService.findUserById(userId);
        boolean isSpeaker = chatRoom.isSpeaker(user);
        return isSpeaker ? chatRoom.getSpeakerUnreadCount() : chatRoom.getListenerUnreadCount();
    }

    private void notifyUnreadCount(Long roomId, Long userId, Long count) {
        Map<String, Object> unreadData = new HashMap<>();
        unreadData.put("roomId", roomId);
        unreadData.put("unreadCount", count);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/unread",
                unreadData
        );
    }

    public void notifyTotalUnreadCount(Long userId) {
        Long totalCount = getTotalUnreadCount(userId);

        String totalUnreadKey = redisKeyManager.getUserTotalUnreadCountKey(userId);
        redisTemplate.opsForValue().set(totalUnreadKey, totalCount);
        redisTemplate.expire(totalUnreadKey, 1, TimeUnit.DAYS);

        Map<String, Object> totalUnreadData = new HashMap<>();
        totalUnreadData.put("totalUnreadCount", totalCount);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/total-unread",
                totalUnreadData
        );
    }
}
