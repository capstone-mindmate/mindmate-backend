package com.mindmate.mindmate_server.global.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DltTopicStatusDTO {
    private String dltTopicName;
    private String originalTopicName;
    private long messageCount;
    private String lastMessageTime;
    private String category;
    private boolean active;
    private List<String> consumerGroups;
    private Map<String, Long> errorCounts;

}
