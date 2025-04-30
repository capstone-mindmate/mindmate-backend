package com.mindmate.mindmate_server.emoticon.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmoticonMessageResponse {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private Long emoticonId;
    private String emoticonUrl;
    private String emoticonName;
    private LocalDateTime timestamp;
    private MessageType type = MessageType.EMOTICON;

    public static EmoticonMessageResponse from(ChatMessage message, Emoticon emoticon, String senderName) {
        return EmoticonMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(senderName)
                .emoticonId(emoticon.getId())
                .emoticonUrl(emoticon.getImageUrl())
                .emoticonName(emoticon.getName())
                .timestamp(message.getCreatedAt())
                .build();
    }
}
