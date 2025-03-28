package com.mindmate.mindmate_server.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSearchRequest {
    private String keyword;
    private Long oldestLoadedMessageId;
    private Long newestLoadedMessageId;
}
