package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import com.mindmate.mindmate_server.magazine.dto.MagazineEngagementEvent;
import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public KafkaAdminClient kafkaAdminClient() {
        return (KafkaAdminClient) AdminClient.create(kafkaAdmin().getConfigurationProperties());
    }

    /**
     * 공통 Producer 설정
     */
    private <T> Map<String, Object> getProducerConfigs() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return config;
    }

    /**
     * 공통 Consumer 설정
     */
    private <T> Map<String, Object> getConsumerConfigs(String groupId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 자동 커밋 비활성화
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return config;
    }

    /**
     * Generic Kafka Template
     */
    @Bean
    public <T> ProducerFactory<String, T> producerFactory() {
        return new DefaultKafkaProducerFactory<>(getProducerConfigs());
    }

    /**
     * Generic Kafka Template
     */
    @Bean
    public <T> KafkaTemplate<String, T> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Consumer Factory 생성
     */
    private <T> ConsumerFactory<String, T> consumerFactory(String groupId, Class<T> valueType) {
        Map<String, Object> props = getConsumerConfigs(groupId);

        JsonDeserializer<T> deserializer = new JsonDeserializer<>(valueType);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Listener Container Factory 생성
     */
    private <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerContainerFactory(String groupId, Class<T> valueType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(groupId, valueType));
        factory.setConcurrency(3);

        // 수동 커밋 모드 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /**
     * 토픽 설정
     */
    private NewTopic createTopic(String name, int partitions, short replicas, Map<String, String> configs) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .configs(configs)
                .build();
    }

    /**
     * 기본 토픽 설정
     */
    private Map<String, String> getDefaultTopicConfigs() {
        return Map.of(
                TopicConfig.RETENTION_MS_CONFIG, "604800000", // 7일 유효 기간
                TopicConfig.CLEANUP_POLICY_CONFIG, "delete"
        );
    }

    /**
     * 모든 DLQ 토픽 자동 생성
     */
    @Bean
    public List<NewTopic> createDlqTopics() {
        List<String> sourceTopics = List.of(
                "chat-message-topic",
                "chat-room-close-topic",
                "magazine-engagement-topic",
                "matching-accepted"
        );

        return sourceTopics.stream()
                .map(topic -> TopicBuilder.name(topic + "-dlq")
                        .partitions(3)
                        .replicas(1)
                        .configs(getDefaultTopicConfigs())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 통합 DLQ 설정
     */
    private <T> DefaultErrorHandler createDefaultErrorHandler(KafkaTemplate<String, T> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    String dlqTopic = record.topic() + "-dlq";
                    log.error("메시지 처리 실패, DLQ로 이동: 토픽={}, 예외={}", dlqTopic, ex.getMessage());
                    return new TopicPartition(dlqTopic, record.partition());
                });

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        errorHandler.setCommitRecovered(true); // DLQ로 이동 후 오프셋 커밋

        return errorHandler;
    }


    /**
     * ChatMessageEvent 처리
     */
    @Bean
    public ConsumerFactory<String, ChatMessageEvent> chatMessageConsumerFactory() {
        return consumerFactory("chat-group", ChatMessageEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> chatMessageListenerContainerFactory(
            KafkaTemplate<String, ChatMessageEvent> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> factory =
                listenerContainerFactory("chat-group", ChatMessageEvent.class);
        factory.setCommonErrorHandler(createDefaultErrorHandler(kafkaTemplate));

        return factory;
    }


    /**
     * ChatRoomEvent 처리
     */
    @Bean
    public ConsumerFactory<String, ChatRoomCloseEvent> chatRoomCloseConsumerFactory() {
        return consumerFactory("chat-room-close-group", ChatRoomCloseEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatRoomCloseEvent> chatRoomCloseListenerContainerFactory(
            KafkaTemplate<String, ChatRoomCloseEvent> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, ChatRoomCloseEvent> factory =
                listenerContainerFactory("chat-room-close-group", ChatRoomCloseEvent.class);
        factory.setCommonErrorHandler(createDefaultErrorHandler(kafkaTemplate));

        return factory;
    }


    
    // MatchingAcceptedEvent 처리
    @Bean
    public ConsumerFactory<String, MatchingAcceptedEvent> matchingConsumerFactory() {
        return consumerFactory("matching-group", MatchingAcceptedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchingAcceptedEvent> kafkaListenerContainerFactory(
            KafkaTemplate<String, MatchingAcceptedEvent> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, MatchingAcceptedEvent> factory =
                listenerContainerFactory("matching-group", MatchingAcceptedEvent.class);
        factory.setCommonErrorHandler(createDefaultErrorHandler(kafkaTemplate));

        return factory;
    }

    /**
     * MagazineEngagementEvent 처리
     */
    @Bean
    public ConsumerFactory<String, MagazineEngagementEvent> magazineEngagementConsumerFactory() {
        return consumerFactory("magazine-engagement-group", MagazineEngagementEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MagazineEngagementEvent> magazineEngagementListenerContainerFactory(
            KafkaTemplate<String, MagazineEngagementEvent> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, MagazineEngagementEvent> factory =
                listenerContainerFactory("magazine-engagement-group", MagazineEngagementEvent.class);
        factory.setCommonErrorHandler(createDefaultErrorHandler(kafkaTemplate));

        return factory;
    }


    /**
     * Topic 처리
     */
    @Bean
    public NewTopic chatMessageTopic() {
        return createTopic("chat-message-topic", 3, (short) 1, getDefaultTopicConfigs());
    }

    @Bean
    public NewTopic chatRoomCloseTopic() {
        return createTopic("chat-room-close-topic", 3, (short) 1, getDefaultTopicConfigs());
    }

    @Bean
    public NewTopic matchingAcceptedTopic() {
        return createTopic("matching-accepted", 3, (short) 1, getDefaultTopicConfigs());
    }

    @Bean
    public NewTopic magazineEngagementTopic() {
        return createTopic("magazine-engagement-topic", 3, (short) 1, getDefaultTopicConfigs());
    }

}