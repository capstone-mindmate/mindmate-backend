package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Chat Message Producer 설정
     */
    @Bean
    public ProducerFactory<String, ChatMessageEvent> chatMessageProducerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Chat Room Close Event Producer 설정
     */
    @Bean
    public ProducerFactory<String, ChatRoomCloseEvent> chatRoomCloseProducerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Chat Message Consumer 설정
     */
    @Bean
    public ConsumerFactory<String, ChatMessageEvent> chatMessageConsumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Chat Room Close Event Consumer 설정
     */
    @Bean
    public ConsumerFactory<String, ChatRoomCloseEvent> chatRoomCloseConsumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-room-close-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Chat Message Kafka Template
     */
    @Bean
    public KafkaTemplate<String, ChatMessageEvent> chatMessageKafkaTemplate() {
        return new KafkaTemplate<>(chatMessageProducerFactory());
    }

    /**
     * Chat Room Close Event Kafka Template
     */
    @Bean
    public KafkaTemplate<String, ChatRoomCloseEvent> chatRoomCloseKafkaTemplate() {
        return new KafkaTemplate<>(chatRoomCloseProducerFactory());
    }


    /**
     * Chat Message Listener Container Factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> chatMessageListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatMessageConsumerFactory());
        return factory;
    }

    /**
     * Chat Room Close Event Listener Container Factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatRoomCloseEvent> chatRoomCloseListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChatRoomCloseEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatRoomCloseConsumerFactory());
        return factory;
    }

    /**
     * Matching Producer 설정
     */
    @Bean
    public ProducerFactory<String, MatchingAcceptedEvent> matchingProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, MatchingAcceptedEvent> matchingKafkaTemplate() {
        return new KafkaTemplate<>(matchingProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, MatchingAcceptedEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "matching-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchingAcceptedEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MatchingAcceptedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);

        return factory;
    }

    /**
     * Kafka 토픽 설정 - 채팅
     */
    @Bean
    public NewTopic chatMessageTopic() {
        return TopicBuilder.name("chat-message-topic")
                .partitions(3)
                .replicas(1)
                .configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG, "604800000",
                        TopicConfig.CLEANUP_POLICY_CONFIG, "delete"
                ))
                .build();
    }

    /**
     * Kafka 토픽 설정 - 채팅방 종료 이벤트
     */
    @Bean
    public NewTopic chatRoomCloseTopic() {
        return TopicBuilder.name("chat-room-close-topic")
                .partitions(3)
                .replicas(1)
                .configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG, "604800000", // 7일 보관
                        TopicConfig.CLEANUP_POLICY_CONFIG, "delete"
                ))
                .build();
    }


    /**
     * Kafka 토픽 설정 - 매칭 이벤트
     */
    @Bean
    public NewTopic matchingAcceptedTopic() {
        return TopicBuilder.name("matching-accepted")
                .partitions(3)
                .replicas(1)
                .build();
    }
}