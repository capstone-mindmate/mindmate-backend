package com.mindmate.mindmate_server.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatCloseRequestNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long chatRoomId;
    private final String requesterNickname;

    @Override
    public String getTitle() {
        return "채팅방 종료 요청";
    }

    @Override
    public String getContent() {
        return String.format("%s님이 채팅방 종료를 요청했습니다.", requesterNickname);
    }

    @Override
    public String getType() {
        return "CHAT_CLOSE_REQUEST";
    }

    @Override
    public Long getRelatedEntityId() {
        return chatRoomId;
    }
}