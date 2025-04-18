package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.chat.domain.ReactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageReactionResponse {
    private Long id;
    private Long messageId;
    private Long userId;
    private String userName;
    private ReactionType reactionType;

    public static MessageReactionResponse from(MessageReaction messageReaction) {
        return MessageReactionResponse.builder()
                .id(messageReaction.getId())
                .messageId(messageReaction.getMessage().getId())
                .userId(messageReaction.getUser().getId())
                .userName(messageReaction.getUser().getProfile().getNickname())
                .reactionType(messageReaction.getReactionType())
                .build();
    }
}
