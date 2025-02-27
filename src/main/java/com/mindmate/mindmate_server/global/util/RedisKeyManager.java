package com.mindmate.mindmate_server.global.util;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyManager {
    // 사용자 상태 관련 키
    public String getUserStatusKey(Long userId) {
        return "user:status:" + userId;
    }

    // 읽지 않은 메시지 카운트 관련 키
    public String getUnreadCountKey(Long roomId, Long userId) {
        return "chat:room:" + roomId + ":unread:" + userId;
    }

    // 채팅방 채널 키
    public String getChatRoomChannel(Long roomId) {
        return "chat:room:" + roomId;
    }

    // 사용자 상태 채널 키
    public String getUserStatusChannel(Long userId) {
        return "user:status:" + userId;
    }

    // 읽음 상태 키
    public String getReadStatusKey(Long roomId, Long userId) {
        return "chat:room:" + roomId + ":read:" + userId;
    }
}
