package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ChatRoomDetailResponse {
    private Long roomId;
    private Long matchingId;
    private LocalDateTime createdAt;
    // todo: 상대방 id?
    private List<ChatMessageResponse> messages;

    public static ChatRoomDetailResponse from(ChatRoom chatRoom, List<ChatMessage> messages, Long userId) {
        return ChatRoomDetailResponse.builder()
                .roomId(chatRoom.getId())
                .matchingId(chatRoom.getMatching().getId())
                .createdAt(chatRoom.getCreatedAt())
                .messages(messages.stream()
                        .map(message -> ChatMessageResponse.from(message, userId))
                        .collect(Collectors.toList()))
                .build();
    }
}
