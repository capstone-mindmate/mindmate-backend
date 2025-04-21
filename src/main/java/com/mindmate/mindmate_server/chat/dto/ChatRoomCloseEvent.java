package com.mindmate.mindmate_server.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCloseEvent {
    private Long chatRoomId;
    private Long speakerId;
    private Long listenerId;
    private LocalDateTime closedAt;

    private Long matchingId;
}
