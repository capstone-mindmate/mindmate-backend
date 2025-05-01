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
    public String getFilteringCountKey(Long userId, Object roomId) {
        return "filtering:count:" + userId + ":" + roomId;
    }

    // 필터링 내용 저장 키? todo: 해당 내용을 따로 db에 저장을 해야할까 아니면 짧게짧게 redis로만 관리할까
    public String getFilteringContentKey(Long userId, Long roomId) {
        return "filtering:content:" + userId + ":" + roomId;
    }

    // 사용자 suspension 정보 관리
    public String getUserSuspensionKey(Long userId) {
        return "user:suspension:" + userId;
    }


    // 매거진 조회수 키
    public String getMagazineViewCountKey(Long magazineId) {
        return "magazine:views:" + magazineId;
    }

    // 매거진 인기도
    public String getMagazinePopularityKey() {
        return "magazine:popularity";
    }

    // 카테고리별 인기 매거진
    public String getCategoryPopularityKey(String category) {
        return "magazine:category:" + category + ":popularity";
    }

    // 사용자 매거진 조회 중복 방지
    public String getMagazineViewedKey(Long userId, Long magazineId) {
        return "magazine:viewed:" + userId + ":" + magazineId;
    }

    // 사용자 매거진 체류 시간
    public String getMagazineDwellTimeKey(Long magazineId) {
        return "magazine:dwell:" + magazineId;
    }

    // 매거진 이벤트 중복 처리 키 관리
    public String getMagazineProcessedEventKey(String eventId) {
        return "magazine:processed:" + eventId;
    }

    public String getUserTotalUnreadCountKey(Long userId) {
        return String.format("user:%d:total-unread", userId);
    }
}
