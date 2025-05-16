package com.mindmate.mindmate_server.global.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class DltMessageDTO {
    private String key;
    private Object value;
    private String timestamp;
    private int partition;
    private long offset;
    private String originalTopic;
    private String errorMessage;
    private String consumerGroup;
    private Map<String, String> headers;

    public static DltMessageDTO from(Map<String, Object> rawMessage, String topic) {
        // 헤더 정보 추출
        Map<String, String> headers = new HashMap<>();
        if (rawMessage.containsKey("headers")) {
            Object headersObj = rawMessage.get("headers");
            if (headersObj instanceof Map) {
                ((Map<?, ?>) headersObj).forEach((k, v) ->
                        headers.put(String.valueOf(k), String.valueOf(v)));
            }
        }

        // 오류 메시지 추출
        String errorMessage = null;
        if (headers.containsKey("error-message")) {
            errorMessage = headers.get("error-message");
        }

        // 컨슈머 그룹 추출
        String consumerGroup = null;
        if (headers.containsKey("consumer-group")) {
            consumerGroup = headers.get("consumer-group");
        }

        return DltMessageDTO.builder()
                .key((String) rawMessage.get("key"))
                .value(rawMessage.get("value"))
                .timestamp((String) rawMessage.get("timestamp"))
                .partition((Integer) rawMessage.get("partition"))
                .offset((Long) rawMessage.get("offset"))
                .originalTopic(topic.replace("-dlt", ""))
                .errorMessage(errorMessage)
                .consumerGroup(consumerGroup)
                .headers(headers)
                .build();
    }
}
