package com.mindmate.mindmate_server.global.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class DlqMessageDTO {
    private String key;
    private Object value;
    private String timestamp;
    private int partition;
    private long offset;

    // 추가 필드
    private String originalTopic;
    private String errorMessage;

    public static DlqMessageDTO from(Map<String, Object> rawMessage, String topic) {
        return DlqMessageDTO.builder()
                .key((String) rawMessage.get("key"))
                .value(rawMessage.get("value"))
                .timestamp((String) rawMessage.get("timestamp"))
                .partition((Integer) rawMessage.get("partition"))
                .offset((Long) rawMessage.get("offset"))
                .originalTopic(topic.replace("-dlq", ""))
                .build();
    }
}
