package com.mindmate.mindmate_server.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReadStatusNotification {
    private Long roomId;
    private Long userId;
    private LocalDateTime timestamp;
}
