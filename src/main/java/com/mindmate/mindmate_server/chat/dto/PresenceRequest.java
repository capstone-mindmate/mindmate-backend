package com.mindmate.mindmate_server.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresenceRequest {
    private String status;
    private Long activeRoomId;
}
