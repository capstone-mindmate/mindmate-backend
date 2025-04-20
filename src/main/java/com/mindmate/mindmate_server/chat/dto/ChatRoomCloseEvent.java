package com.mindmate.mindmate_server.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomCloseEvent {
    private Long chatRoomId;
    private Long speakerId;
    private Long listenerId;
    private LocalDateTime closedAt;

    private Long matchingId;
}
