package com.mindmate.mindmate_server.global.util;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka 컨슈머 팩토리 구현체
 */
@Component
@RequiredArgsConstructor
public class KafkaConsumerFactory {
    private final ConsumerFactory<String, Object> consumerFactory;

    public Consumer<String, Object> createConsumer() {
        return consumerFactory.createConsumer("admin-group", "admin-client-" + UUID.randomUUID());
    }
}