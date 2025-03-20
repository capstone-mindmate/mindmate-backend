package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.domain.ReactionType;
import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long roomId; // 겹치지 않나?
    private Long senderId;

    private String senderName;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;

    private Map<ReactionType, Integer> reactionCounts;
    private ReactionType userReaction;


    public static ChatMessageResponse from(ChatMessage message, Long currentUserId) {
        Map<ReactionType, Integer> reactionCounts = new HashMap<>();
        for (MessageReaction reaction : message.getMessageReactions()) {
            if (reaction.getReactionType() != null) {
                reactionCounts.put(reaction.getReactionType(),
                        reactionCounts.getOrDefault(reaction.getReactionType(), 0) + 1);
            }
        }

        ReactionType userReaction = null;
        if (currentUserId != null) {
            userReaction = message.getMessageReactions().stream()
                    .filter(r -> r.getUser().getId().equals(currentUserId))
                    .map(MessageReaction::getReactionType)
                    .findFirst()
                    .orElse(null);
        }

        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getProfile().getNickname())
                .content(message.getFilteredContent() != null ? message.getFilteredContent() : message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .reactionCounts(reactionCounts)
                .userReaction(userReaction)
                .build();
    }

}
