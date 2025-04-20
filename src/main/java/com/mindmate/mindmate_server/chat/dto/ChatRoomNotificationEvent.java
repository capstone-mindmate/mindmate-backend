package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatRoomCloseType;
import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomNotificationEvent implements NotificationEvent {
    private Long recipientId;
    private Long chatRoomId;
    private final ChatRoomCloseType closeType;
    // private final String senderName 상대방 이름이 익명인 경우와 닉네임인 경우 있으니까 굳이 보여주지 않기

    @Override
    public String getTitle() {
        switch (closeType) {
            case ACCEPT:
                return "채팅방 종료 완료";
            case REQUEST:
                return "채팅방 종료 요청";
            case REJECT:
                return "채팅방 종료 요청 거절";
            default:
                return "채팅방 알림";
        }
    }

    @Override
    public String getContent() {
        switch (closeType) {
            case REQUEST:
                return "상대방이 채팅방 종료를 요청했습니다.";
            case ACCEPT:
                return "상대방과 상담이 종료되었습니다. 상담에 대한 리뷰를 작성해주세요.";
            case REJECT:
                return "상대방이 채팅방 종료 요청을 거절했습니다.";
            default:
                return "채팅방 상태가 변경되었습니다.";
        }
    }

    @Override
    public NotificationType getType() {
        return NotificationType.CHAT_CLOSED;
    }

    @Override
    public Long getRelatedEntityId() {
        return chatRoomId;
    }
}
