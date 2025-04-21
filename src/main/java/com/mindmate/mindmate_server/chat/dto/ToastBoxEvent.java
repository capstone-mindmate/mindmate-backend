package com.mindmate.mindmate_server.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToastBoxEvent {
    private Long roomId;
    private Long messageId;
    private String keyword;
    private String title;
    private String content;
    private String linkUrl;
    private String imageUrl;
}
