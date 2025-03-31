package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.service.UserService;
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
    private final RedisKeyManager redisKeyManager;

    private final UserService userService;
    private final ChatRoomService chatRoomService;
    /**
     * Redis 값 관리 정리
     * [사용자 상태]
     * - 키: user:status:{userId}
     * - 값: Hash 형태로 저장 (온라인 상태, 활성 채팅방 ID, 마지막 활동 시간, status - 향후 away 고려?)
     * - 변경 시점: websocket 연결/해제 시 + 사용자가 채팅방에 입장/퇴장할 때
     * - 만료 시간: 온라인이면 5분, 오프라인이면 30분
     *
     * [읽음 상태]
     * - 키: chat:room:{roomId}:user:{userId}:read
     * - 값: 마지막으로 읽은 시간
     * - 변경 시점: 사용자가 메시지를 읽을 때
     * - 만료 시간: 1일
     *
     * [미읽음 상태]
     * - 키: chat:room:{roomId}:user:{userId}:unread
     * - 값: 미읽음 메시지 수
     * - 변경 시점: 새 메시지 도착 시 증가 + 사용자가 메시지 읽을 때 리셋
     * - 만료 시간: 설정 x
     */

    public void updateUserStatus(Long userId, boolean isOnline, Long activeRoomId) {
        String statusKey = redisKeyManager.getUserStatusKey(userId);
        Map<String, Object> status = new HashMap<>();
        status.put("online", isOnline);
        status.put("activeRoomId", activeRoomId);
        status.put("lastActive", LocalDateTime.now().toString());
        status.put("status", isOnline ? "ONLINE" : "OFFLINE");  // ONLINE, AWAY 등

        redisTemplate.opsForHash().putAll(statusKey, status);

        if (isOnline) {
            redisTemplate.expire(statusKey, 5, TimeUnit.MINUTES);
        } else {
            redisTemplate.expire(statusKey, 30, TimeUnit.MINUTES);
        }

        String channel = redisKeyManager.getUserStatusChannel(userId);
        redisTemplate.convertAndSend(channel, status);
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

    /**
     * Redis에서만 미읽음 카운트 증가
     */
    public Long incrementUnreadCountInRedis(Long roomId, Long userId) {
        // Redis 업데이트
        String unreadKey = redisKeyManager.getUnreadCountKey(roomId, userId);
        Long count = redisTemplate.opsForValue().increment(unreadKey);

        // WebSocket 알림
        notifyUnreadCount(roomId, userId, count);

        log.info("Incremented unread count in Redis for user {} in room {} to {}", userId, roomId, count);
        return count;
    }

    // db 관련은 여기서 처리하지 않도록 설정
//    @Transactional
//    public void incrementUnreadCount(Long roomId, Long userId) {
//        ChatRoom chatRoom = chatRoomService.findChatRoomById(roomId);
//        // Redis 업데이트
//        String unreadKey = redisKeyManager.getUnreadCountKey(roomId, userId);
//        Long count = redisTemplate.opsForValue().increment(unreadKey);
//        User user = userService.findUserById(userId);
//
//        // WebSocket 알림
//        notifyUnreadCount(roomId, userId, count);
//
//        log.info("Before increase: {}", chatRoom.getListenerUnreadCount());
//        // DB 업데이트 (트랜잭션 내에서)
//        chatRoom.increaseUnreadCount(user);
////            chatRoom.increaseUnreadCountForSpeaker();
////        } else {
////            chatRoom.increaseUnreadCountForListener();
////        }
//
////        chatRoomRepository.saveAndFlush(chatRoom);
//        chatRoomService.save(chatRoom);
//        log.info("Incremented unread count for user {} in room {} to {}", userId, roomId, count);
//    }

    public void resetUnreadCount(Long roomId, Long userId) {
        String unreadKey = redisKeyManager.getUnreadCountKey(roomId, userId);
        redisTemplate.delete(unreadKey);

        notifyUnreadCount(roomId, userId, 0L);
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

//    public String getUserStatus(Long userId) {
//        String statusKey = redisKeyManager.getUserStatusKey(userId);
//        Boolean isOnline = (Boolean) redisTemplate.opsForHash().get(statusKey, "online");
//        return Boolean.TRUE.equals(isOnline) ? "ONLINE" : "OFFLINE";
//    }

//        public void sendNotification(Long userId, Object notification) {
//        messagingTemplate.convertAndSendToUser(
//                userId.toString(),
//                "/queue/notification",
//                notification
//        );
//    }
//
//    public boolean shouldIncrementUnreadCount(Long userId, Long roomId) {
//        return !isUserActiveInRoom(userId, roomId);
//    }
}
