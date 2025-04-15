package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
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
public class ChatMessageEvent {
    private Long messageId;
    private LocalDateTime timestamp;
    private Long roomId;
    private Long senderId;
    private String content;
    private MessageType type;

    private Long recipientId;
    private boolean recipientActive;

    private boolean filtered;
    private FilteringWordCategory filteringWordCategory;

    private boolean encrypted;
    private String plainContent;

    public void setId(Long id) {
        this.messageId = id;
    }
}
