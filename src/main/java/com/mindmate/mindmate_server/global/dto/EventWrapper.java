package com.mindmate.mindmate_server.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventWrapper<T> {
    private String topic;
    private String key;
    private T event;
//    private ConsumerRecord<String, T> record; // 원본 ConsumerRecord 처리?
}
