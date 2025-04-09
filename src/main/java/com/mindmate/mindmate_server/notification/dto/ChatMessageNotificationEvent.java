package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long chatRoomId;
    private final String senderName;
    private final String messagePreview;

    @Override
    public String getTitle() {
        return "새 메시지";
    }

    @Override
    public String getContent() {
        return String.format("'%s': %s", senderName, messagePreview);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.CHAT_MESSAGE;
    }

    @Override
    public Long getRelatedEntityId() {
        return chatRoomId;
    }

    @Override
    public boolean saveToDatabase() {
        return false;
    }

    @Override
    public boolean sendFCM() {
        return true;
    }
}