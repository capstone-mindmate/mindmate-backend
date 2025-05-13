package com.mindmate.mindmate_server.global.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DlqTopicStatusDTO {
    private String dlqTopicName;
    private String originalTopicName;
    private long messageCount;
    private String lastMessageTime;
    private String category;
    private boolean active;

}
