package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MagazineRejectedNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long magazineId;
    private final String magazineTitle;
    private final String rejectionReason;

    @Override
    public String getTitle() {
        return "매거진 반려";
    }

    @Override
    public String getContent() {
        if (rejectionReason != null && !rejectionReason.isEmpty()) {
            return String.format("'%s' 매거진이 반려되었습니다. 사유: %s", magazineTitle, rejectionReason);
        }
        return String.format("'%s' 매거진이 반려되었습니다.", magazineTitle);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.MAGAZINE_REJECTED;
    }

    @Override
    public Long getRelatedEntityId() {
        return magazineId;
    }
}
