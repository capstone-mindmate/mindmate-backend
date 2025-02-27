package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedisKeyManager redisKeyManager;

    /**
     * todo
     * 1. read status 활용
     * 2. read/unread 관리
     * 3. 다른곳에 redis 활용하는곳 manager 관리
     * 4. securityUtil 동작 확인
     * 5. consumer에서 chatevent id 관리
     * 6.
     */

    public void updateUserStatus(Long userId, boolean isOnline, Long activeRoomId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
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

        String channel = redisKeyManager.getUserStatusChannel(userId);
        redisTemplate.convertAndSend(channel, status);

        log.info("User status updated: userId={}, online={}, activeRoom={}", userId, isOnline, activeRoomId);
    }

    public String getUserStatus(Long userId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
        Boolean isOnline = (Boolean) redisTemplate.opsForHash().get(statusKey, "online");
        return Boolean.TRUE.equals(isOnline) ? "ONLINE" : "OFFLINE";
    }

    public Long getActiveRoom(Long userId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
        return (Long) redisTemplate.opsForHash().get(statusKey, "activeRoomId");
    }

    @Transactional
    public void incrementUnreadCount(Long roomId, Long userId, ChatRoom chatRoom, RoleType senderRole) {
        // Redis 업데이트
        String unreadKey = redisKeyManager.getUnreadCountKey(roomId, userId);
        Long count = redisTemplate.opsForValue().increment(unreadKey);

        // WebSocket 알림
        notifyUnreadCount(roomId, userId, count);

        // DB 업데이트 (트랜잭션 내에서)
        if (senderRole == RoleType.ROLE_LISTENER) {
            chatRoom.increaseUnreadCountForSpeaker();
        } else {
            chatRoom.increaseUnreadCountForListener();
        }
    }

    public void resetUnreadCount(Long roomId, Long userId) {
        String unreadKey = redisKeyManager.getUnreadCountKey(roomId, userId);
        redisTemplate.delete(unreadKey);

        notifyUnreadCount(roomId, userId, 0L);
    }

    public void sendNotification(Long userId, Object notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notification",
                notification
        );
    }

    public boolean shouldIncrementUnreadCount(Long userId, Long roomId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
        Boolean isOnline = (Boolean) redisTemplate.opsForHash().get(statusKey, "online");
        Long activeRoomId = (Long) redisTemplate.opsForHash().get(statusKey, "activeRoomId");

        // 오프라인이거나 다른 채팅방을 보고 있는 경우 true 반환
        return isOnline == null || !isOnline || activeRoomId == null || !activeRoomId.equals(roomId);
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
}
