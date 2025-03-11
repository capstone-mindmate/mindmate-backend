package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ChatRoomDetailResponse {
    private Long roomId;
//    private String matchingName;
    private List<ChatMessageResponse> messages;

    public static ChatRoomDetailResponse from(ChatRoom chatRoom, List<ChatMessage> messages) {
        return ChatRoomDetailResponse.builder()
                .roomId(chatRoom.getId())
                .messages(messages.stream()
                        .map(ChatMessageResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
