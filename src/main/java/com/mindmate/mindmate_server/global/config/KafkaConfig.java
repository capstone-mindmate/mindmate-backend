package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import com.mindmate.mindmate_server.global.service.DltReprocessingService;
import com.mindmate.mindmate_server.global.util.GlobalDltHandler;
import com.mindmate.mindmate_server.global.util.KafkaTopicUtils;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.magazine.dto.MagazineEngagementEvent;
import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.EndpointHandlerMethod;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
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
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }


    /**
     * 공통 Producer 설정
     */
    private <T> Map<String, Object> getProducerConfigs() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

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
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

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
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        JsonDeserializer<T> deserializer = new JsonDeserializer<>(valueType);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        deserializer.setRemoveTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer)
        );
    }

    @Bean
    public ConsumerFactory<String, Object> objectConsumerFactory() {
        Map<String, Object> props = getConsumerConfigs("admin-group");
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(Object.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer)
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

    @Bean
    public GlobalDltHandler globalDltHandler(KafkaTopicUtils kafkaTopicUtils, SlackNotifier slackNotifier, DltReprocessingService reprocessingService) {
        return new GlobalDltHandler(kafkaTopicUtils, slackNotifier, reprocessingService);
    }

    /**
     * Global 재시도 토픽 설정
     */
    @Bean
    public RetryTopicConfiguration retryTopicConfiguration(KafkaTemplate<String, Object> template, GlobalDltHandler globalDltHandler) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .maxAttempts(3)
                .fixedBackOff(1000)
                .includeTopic("chat-message-topic")
                .includeTopic("chat-room-close-topic")
                .includeTopic("matching-accepted")
                .includeTopic("magazine-engagement-topic")
                .setTopicSuffixingStrategy(TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
                .dltProcessingFailureStrategy(DltStrategy.FAIL_ON_ERROR)
                .dltHandlerMethod(new EndpointHandlerMethod(globalDltHandler, "handleDltMessage"))
                .create(template);
    }

    /**
     * ChatMessageEvent 처리
     */
    @Bean
    public ConsumerFactory<String, ChatMessageEvent> chatMessageConsumerFactory() {
        return consumerFactory("chat-group", ChatMessageEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> chatMessageListenerContainerFactory() {
        return listenerContainerFactory("chat-group", ChatMessageEvent.class);
    }


    /**
     * ChatRoomEvent 처리
     */
    @Bean
    public ConsumerFactory<String, ChatRoomCloseEvent> chatRoomCloseConsumerFactory() {
        return consumerFactory("chat-room-close-group", ChatRoomCloseEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatRoomCloseEvent> chatRoomCloseListenerContainerFactory() {
        return listenerContainerFactory("chat-room-close-group", ChatRoomCloseEvent.class);
    }


    // MatchingAcceptedEvent 처리
    @Bean
    public ConsumerFactory<String, MatchingAcceptedEvent> matchingConsumerFactory() {
        return consumerFactory("matching-group", MatchingAcceptedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchingAcceptedEvent> kafkaListenerContainerFactory() {
        return listenerContainerFactory("matching-group", MatchingAcceptedEvent.class);
    }

    /**
     * MagazineEngagementEvent 처리
     */
    @Bean
    public ConsumerFactory<String, MagazineEngagementEvent> magazineEngagementConsumerFactory() {
        return consumerFactory("magazine-engagement-group", MagazineEngagementEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MagazineEngagementEvent> magazineEngagementListenerContainerFactory() {
        return listenerContainerFactory("magazine-engagement-group", MagazineEngagementEvent.class);
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