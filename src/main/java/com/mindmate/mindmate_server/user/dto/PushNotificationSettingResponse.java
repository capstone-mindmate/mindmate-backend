package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PushNotificationSettingResponse {
    private long userId;
    private boolean pushNotificationEnabled;

    public static PushNotificationSettingResponse of(User user) {
        return PushNotificationSettingResponse.builder()
                .userId(user.getId())
                .pushNotificationEnabled(user.isPushNotificationEnabled())
                .build();
    }
}
