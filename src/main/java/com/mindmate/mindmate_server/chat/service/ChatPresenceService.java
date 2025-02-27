package com.mindmate.mindmate_server.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatPresenceService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public void updateUserStatus(Long userId, boolean isOnline, Long activeRoomId) {
        String statusKey = "user:status:" + userId;
        Map<String, Object> status = new HashMap<>();
        status.put("online", isOnline);
        status.put("activeRoomId", activeRoomId);
        status.put("lastActive", LocalDateTime.now().toString());

        redisTemplate.opsForHash().putAll(statusKey, status);

        if (isOnline) {
            redisTemplate.expire(statusKey, 30, TimeUnit.MINUTES);
        } else {
            redisTemplate.expire(statusKey, 1, TimeUnit.HOURS);
        }

        String channel = "user:status:" + userId;
        redisTemplate.convertAndSend(channel, status);

        log.info("User status updated: userId={}, online={}, activeRoom={}", userId, isOnline, activeRoomId);
    }

    public String getUserStatus(Long userId) {
        String statusKey = "user:status:" + userId;
        Boolean isOnline = (Boolean) redisTemplate.opsForHash().get(statusKey, "online");
        return Boolean.TRUE.equals(isOnline) ? "ONLINE" : "OFFLINE";
    }

    public Long getActiveRoom(Long userId) {
        String statusKey = "user:status:" + userId;
        return (Long) redisTemplate.opsForHash().get(statusKey, "activeRoomId");
    }

    public void incrementUnreadCount(Long roomId, Long userId) {
        String unreadKey = "chat:room:" + roomId + ":unread:" + userId;
        Long count = redisTemplate.opsForValue().increment(unreadKey);

        Map<String, Object> unreadData = new HashMap<>();
        unreadData.put("roomId", roomId);
        unreadData.put("unreadCount", count);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/unread",
                unreadData
        );
    }

    public void resetUnreadCount(Long roomId, Long userId) {
        String unreadKey = "chat:room:" + roomId + ":unread:" + userId;
        redisTemplate.delete(unreadKey);

        Map<String, Object> unreadData = new HashMap<>();
        unreadData.put("roomId", roomId);
        unreadData.put("unreadCount", 0);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/unread",
                unreadData
        );
    }

    public void sendNotification(Long userId, Object notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notification",
                notification
        );
    }
}
