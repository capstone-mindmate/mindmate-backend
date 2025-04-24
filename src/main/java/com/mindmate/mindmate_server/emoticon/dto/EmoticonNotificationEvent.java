package com.mindmate.mindmate_server.emoticon.dto;

import com.mindmate.mindmate_server.emoticon.domain.EmoticonStatus;
import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmoticonNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long emoticonId;
    private final String emoticonName;
    private final EmoticonStatus status;

    @Override
    public String getTitle() {
        return switch (status) {
            case ACCEPT -> "이모티콘 등록 승인";
            case REJECT -> "이모티콘 등록 거절";
            case PENDING -> "이모틴 등록 대기";
        };
    }

    @Override
    public String getContent() {
        return switch (status) {
            case ACCEPT -> String.format("'%s' 등록이 수락되었습니다.", emoticonName);
            case REJECT -> String.format("'%s' 등록이 거절되었습니다.", emoticonName);
            case PENDING -> String.format("'%s' 등록이 대기중입니다.", emoticonName);
        };
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMOTICON_UPLOAD;
    }

    @Override
    public Long getRelatedEntityId() {
        return emoticonId;
    }
}
