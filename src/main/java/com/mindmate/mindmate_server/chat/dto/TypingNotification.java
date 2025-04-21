package com.mindmate.mindmate_server.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TypingNotification {
    private Long roomId;
    private Long userId;
    private boolean typing;
}
