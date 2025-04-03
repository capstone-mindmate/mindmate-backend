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
public class ChatEvent<T> {
    private ChatEventType type;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ChatEvent<T> of(ChatEventType type, T data) {
        return ChatEvent.<T>builder()
                .type(type)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
