package com.mindmate.mindmate_server.global.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KafkaTopic {
    CHAT_MESSAGE("chat-message-topic"),
    CHAT_ROOM_CLOSE("chat-room-close-topic"),
    MAGAZINE_ENGAGEMENT("magazine-engagement-topic"),
    MATCHING_ACCEPTED("matching-accepted");

    private final String topicName;

    public String getDlqName() {
        return topicName + "-dlq";
    }
}
