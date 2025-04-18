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

    // 사용자별 채팅방 필터링 횟수 키
    public String getFilteringCountKey(Long userId, Long roomId) {
        return "filtering:count:" + userId + ":" + roomId;
    }

    // 필터링 내용 저장 키? todo: 해당 내용을 따로 db에 저장을 해야할까 아니면 짧게짧게 redis로만 관리할까
    public String getFilteringContentKey(Long userId, Long roomId) {
        return "filtering:content:" + userId + ":" + roomId;
    }

}
