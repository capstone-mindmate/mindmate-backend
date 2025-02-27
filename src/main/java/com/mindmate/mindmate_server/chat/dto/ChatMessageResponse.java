package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long roomId;
    private Long senderId;

    private String senderName;
    private RoleType senderRole;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSenderRole() == RoleType.ROLE_LISTENER
                        ? message.getChatRoom().getListener().getNickname()
                        : message.getChatRoom().getSpeaker().getNickname())
                .senderRole(message.getSenderRole())
                .content(message.getFilteredContent() != null ? message.getFilteredContent() : message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .build();
    }

}
