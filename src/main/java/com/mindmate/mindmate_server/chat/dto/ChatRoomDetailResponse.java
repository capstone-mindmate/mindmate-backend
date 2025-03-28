package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.user.domain.User;
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
    private ChatRoomStatus roomStatus;
    private InitiatorType closeRequestRole;
    private LocalDateTime closeRequestedAt;
    private boolean isListener;
    // todo: 상대방 id?
    private List<ChatMessageResponse> messages;

    public static ChatRoomDetailResponse from(ChatRoom chatRoom, List<ChatMessage> messages, User user) {
        return ChatRoomDetailResponse.builder()
                .roomId(chatRoom.getId())
                .matchingId(chatRoom.getMatching() != null ? chatRoom.getMatching().getId() : null)
                .createdAt(chatRoom.getCreatedAt())
                .roomStatus(chatRoom.getChatRoomStatus())
                .closeRequestRole(chatRoom.getClosureRequesterRole())
                .closeRequestedAt(chatRoom.getClosureRequestAt())
                .isListener(chatRoom.isListener(user))
                .messages(messages.stream()
                        .map(message -> ChatMessageResponse.from(message, user.getId()))
                        .collect(Collectors.toList()))
                .build();
    }
}
