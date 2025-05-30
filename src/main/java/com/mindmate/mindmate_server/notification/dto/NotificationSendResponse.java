package com.mindmate.mindmate_server.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationSendResponse {
    private String message;
    private boolean success;
    private int sentCount;
}