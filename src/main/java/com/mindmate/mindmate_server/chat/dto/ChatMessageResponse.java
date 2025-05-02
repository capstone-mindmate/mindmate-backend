package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.domain.ReactionType;
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
    private Long senderId;

    private String senderName;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;

    private Map<ReactionType, Integer> reactionCounts;
    private ReactionType userReaction;

    private boolean error;
    private String errorMessage;
    private boolean filtered;

    private CustomFormResponse customForm;

    private Long emoticonId;
    private String emoticonUrl;
    private String emoticonName;

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

        CustomFormResponse customFormResponse = null;
        if (message.isCustomForm() && message.getCustomForm() != null) {
            customFormResponse = CustomFormResponse.from(message.getCustomForm());
        }

        Long emoticonId = null;
        String emoticonUrl = null;
        String emoticonName = null;
        if (message.getType() == MessageType.EMOTICON && message.getEmoticon() != null) {
            emoticonId = message.getEmoticon().getId();
            emoticonUrl = message.getEmoticon().getImageUrl();
            emoticonName = message.getEmoticon().getName();
        }

        return ChatMessageResponse.builder()
                .id(message.getId())
//                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getProfile() != null ? message.getSender().getProfile().getNickname() : "Unknown")
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .reactionCounts(reactionCounts)
                .userReaction(userReaction)
                .error(false)
                .customForm(customFormResponse)
                .emoticonId(emoticonId)
                .emoticonUrl(emoticonUrl)
                .emoticonName(emoticonName)
                .build();
    }

    // 필터링 걸린 경우
    public static ChatMessageResponse filteredResponse(Long roomId, Long senderId, String senderName,
                                                       String filteredContent, MessageType type) {
        return ChatMessageResponse.builder()
//                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(filteredContent)
                .type(type)
                .filtered(true)
                .createdAt(LocalDateTime.now())
                .reactionCounts(new HashMap<>())
                .error(false)
                .build();
    }

    // 저장 에러
    public static ChatMessageResponse errorResponse(Long roomId, Long senderId, String senderName,
                                                    String content, MessageType type, String errorMessage) {
        return ChatMessageResponse.builder()
//                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .type(type)
                .createdAt(LocalDateTime.now())
                .reactionCounts(new HashMap<>())
                .error(true)
                .errorMessage(errorMessage)
                .build();
    }

}
