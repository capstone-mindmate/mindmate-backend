package com.mindmate.mindmate_server.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReprocessingService {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaAdminClient kafkaAdminClient;


    /**
     * DLQ 메시지를 원본 토픽을 재전송
     */
    public void reprocessDlqMessage(String dlqTopic, int maxMessages) {
        String originalTopic = dlqTopic.replace("-dlq", "");
        log.info("DLQ {} 에서 최대 {} 개의 메시지를 {} 로 재처리합니다", dlqTopic, maxMessages, originalTopic);

        // 컨슈머 설정 todo: props를 여기서?
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-reprocessor");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        try (KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props)) {
            // DLQ 토픽 구독
            consumer.subscribe(Collections.singletonList(dlqTopic));
            
            int processCount = 0;
            boolean running = true;
            
            while (running && processCount < maxMessages) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, Object> record : records) {
                    try {
                        // 원본 토픽으로 재발행
                        kafkaTemplate.send(originalTopic, record.key(), record.value()).get();
                        log.info("메시지 재처리 성공: {}", record.value());
                        processCount++;
                        
                        if (processCount >= maxMessages) {
                            running = false;
                            break;
                        }
                    } catch (Exception e) {
                        log.error("메시지 재처리 실패: {}", e.getMessage(), e);
                    }
                }

                // 수동 커밋 진행
                consumer.commitSync();
            }
            log.info("DLQ {} 에서 총 {} 개의 메시지를 재처리했습니다", dlqTopic, processCount);
        } catch (Exception e) {
            log.error("DLQ 재처리 중 오류 발생", e);
        }
    }

    /**
     * 특정 DLQ 토픽 모든 메시지 삭제
     */
    public void purgeDlqMessages(String dlqTopic) {
        try {
            kafkaAdminClient.deleteTopics(Collections.singleton(dlqTopic)).all().get();

            waitForTopicDeletion(dlqTopic);

            NewTopic newTopic = new NewTopic(dlqTopic, 3, (short) 1);
            kafkaAdminClient.createTopics(Collections.singleton(newTopic)).all().get();

            log.info("DLQ {} 삭제 및 재생성 완료", dlqTopic);
        } catch (Exception e) {
            log.error("DLQ 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("DLQ 삭제 실패", e);
        }
    }

    private void waitForTopicDeletion(String topic) throws Exception {
        int maxRetries = 10;
        int retryIntervalMs = 1000;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Set<String> topics = kafkaAdminClient.listTopics().names().get();
                if (!topics.contains(topic)) {
                    log.info("토픽 {} 삭제 완료", topic);
                    return;
                }

                log.info("토픽 {} 삭제 대기 중... (시도: {}/{}", topic, retryCount + 1, maxRetries);
                Thread.sleep(retryIntervalMs);
                retryCount++;
            } catch (Exception e) {
                log.warn("토픽 삭제 상태 확인 중 오류: {}", e.getMessage());
                Thread.sleep(retryIntervalMs);
                retryCount++;
            }
        }
        throw new TimeoutException("토픽 " + topic + " 삭제 대기 시간 초과");
    }
}
