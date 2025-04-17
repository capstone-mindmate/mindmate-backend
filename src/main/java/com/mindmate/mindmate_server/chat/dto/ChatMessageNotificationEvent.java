package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long senderId;
    private final String senderName;
    private final Long roomId;
    private final String messageContent;
    private final Long messageId;

    @Override
    public String getTitle() {
        return String.format("새 메시지: %s", senderName);
    }

    @Override
    public String getContent() {
        return messageContent.length() > 20
                ? messageContent.substring(0, 19) + "..."
                : messageContent;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.CHAT_MESSAGE;
    }

    @Override
    public Long getRelatedEntityId() {
        return roomId;
    }
}
