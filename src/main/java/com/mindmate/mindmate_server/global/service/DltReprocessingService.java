package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.util.KafkaConsumerFactory;
import com.mindmate.mindmate_server.global.util.KafkaTopicUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DltReprocessingService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConsumerFactory consumerFactory;
    private final KafkaTopicUtils topicUtils;
    private final AdminClient adminClient;

    /**
     * DLT 메시지 재처리
     */
    public int reprocessDltMessage(String dltTopic, int maxMessages) {
        String originalTopic = topicUtils.extractOriginalTopic(dltTopic);
        String consumerGroup = topicUtils.extractConsumerGroup(dltTopic);

        if (originalTopic == null || originalTopic.equals(dltTopic)) {
            throw new IllegalArgumentException("유효하지 않은 DLT 토픽: " + dltTopic);
        }

        log.info("DLT 메시지 재처리 시작: dltTopic={}, originalTopic={}, consumerGroup={}, maxMessages={}",
                dltTopic, originalTopic, consumerGroup, maxMessages);

        try (Consumer<String, Object> consumer = consumerFactory.createConsumer()) {
            List<TopicPartition> partitions = consumer.partitionsFor(dltTopic).stream()
                    .map(info -> new TopicPartition(dltTopic, info.partition()))
                    .collect(Collectors.toList());

            if (partitions.isEmpty()) {
                log.warn("DLT 토픽 {}에 파티션이 없습니다.", dltTopic);
                return 0;
            }

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            int processedCount = 0;
            int retryCount = 0;
            final int MAX_RETRIES = 3;

            while (processedCount < maxMessages && retryCount < MAX_RETRIES) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(5));

                if (records.isEmpty()) {
                    retryCount++;
                    continue;
                }

                retryCount = 0;

                for (ConsumerRecord<String, Object> record : records) {
                    try {
                        // 원본 토픽으로 메시지 재발행
                        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                                originalTopic, record.key(), record.value());

                        // 필요한 헤더 복사
                        for (Header header : record.headers()) {
                            String key = header.key();
                            if (!key.equals(KafkaHeaders.EXCEPTION_MESSAGE) &&
                                    !key.equals(KafkaHeaders.EXCEPTION_STACKTRACE) &&
                                    !key.equals(KafkaHeaders.EXCEPTION_CAUSE_FQCN)) {
                                producerRecord.headers().add(header);
                            }
                        }

                        // 재처리 헤더 추가
                        producerRecord.headers().add(
                                new RecordHeader("X-Reprocessed-From-DLT", dltTopic.getBytes(StandardCharsets.UTF_8)));
                        producerRecord.headers().add(
                                new RecordHeader("X-Reprocessed-Time",
                                        Instant.now().toString().getBytes(StandardCharsets.UTF_8)));

                        if (consumerGroup != null) {
                            producerRecord.headers().add(
                                    new RecordHeader("X-Target-Consumer-Group",
                                            consumerGroup.getBytes(StandardCharsets.UTF_8)));
                        }

                        kafkaTemplate.send(producerRecord).get();
                        processedCount++;

                        log.info("메시지 재처리 완료: dltTopic={}, originalTopic={}, consumerGroup={}, key={}, offset={}",
                                dltTopic, originalTopic, consumerGroup, record.key(), record.offset());

                        if (processedCount >= maxMessages) {
                            break;
                        }
                    } catch (Exception e) {
                        log.error("메시지 재처리 실패: dltTopic={}, originalTopic={}, key={}, error={}",
                                dltTopic, originalTopic, record.key(), e.getMessage(), e);
                    }
                }
            }

            log.info("DLT 메시지 재처리 완료: dltTopic={}, originalTopic={}, processedCount={}",
                    dltTopic, originalTopic, processedCount);

            return processedCount;
        } catch (Exception e) {
            log.error("DLT 메시지 재처리 실패: {}", e.getMessage(), e);
            throw new RuntimeException("DLT 메시지 재처리 실패", e);
        }
    }

    /**
     * 특정 컨슈머 그룹의 DLT 메시지 재처리
     */
    public int reprocessDltForGroup(String groupId, int maxMessages) {
        List<String> dltTopics = findDltTopicsForGroup(groupId);

        if (dltTopics.isEmpty()) {
            log.warn("컨슈머 그룹 {}에 대한 DLT 토픽이 없습니다.", groupId);
            return 0;
        }

        int totalProcessed = 0;
        int remainingMessages = maxMessages;

        for (String dltTopic : dltTopics) {
            if (remainingMessages <= 0) {
                break;
            }

            int processed = reprocessDltMessage(dltTopic, remainingMessages);
            totalProcessed += processed;
            remainingMessages -= processed;
        }

        return totalProcessed;
    }

    /**
     * DLT 메시지 삭제
     */
    public void purgeDltMessages(String dltTopic) {
        try {
            boolean topicExists = adminClient.listTopics().names().get().contains(dltTopic);

            if (!topicExists) {
                log.warn("DLT 토픽 {}이(가) 존재하지 않습니다.", dltTopic);
                return;
            }

            // 토픽 설정 조회
            ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, dltTopic);
            Map<ConfigResource, Config> configs = adminClient.describeConfigs(
                    Collections.singleton(configResource)).all().get();

            // 토픽 삭제 및 재생성
            adminClient.deleteTopics(Collections.singleton(dltTopic)).all().get();
            log.info("DLT 토픽 {} 삭제 완료", dltTopic);

            // 토픽이 완전히 삭제될 때까지 대기
            waitForTopicDeletion(dltTopic);

            // 토픽 재생성
//             createTopic(dltTopic, configs.get(configResource));

            log.info("DLT 토픽 {} 메시지 삭제 완료", dltTopic);
        } catch (Exception e) {
            log.error("DLT 메시지 삭제 실패: {}", e.getMessage(), e);
            throw new RuntimeException("DLT 메시지 삭제 실패", e);
        }
    }

    /**
     * 토픽이 삭제될 때까지 대기
     */
    private void waitForTopicDeletion(String topic) throws InterruptedException, ExecutionException {
        int maxRetries = 10;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            boolean exists = adminClient.listTopics().names().get().contains(topic);
            if (!exists) {
                return;
            }

            retryCount++;
            Thread.sleep(1000);
        }

        log.warn("토픽 {} 삭제 대기 시간 초과", topic);
    }

    /**
     * 특정 컨슈머 그룹과 관련된 DLT 토픽 찾기
     */
    public List<String> findDltTopicsForGroup(String groupId) {
        try {
            List<String> allTopics = getAllTopics();

            // 그룹 ID에서 접두사 추출
            String groupPrefix = groupId;
            if (groupId.endsWith("-group")) {
                groupPrefix = groupId.substring(0, groupId.length() - 6);
            }

            final String searchPattern = "-" + groupPrefix + "-group-dlt$";

            return allTopics.stream()
                    .filter(topic -> topic.matches(".*" + searchPattern))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("컨슈머 그룹 {} 관련 DLT 토픽 조회 실패: {}", groupId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * DLT 토픽에서 컨슈머 그룹 이름 추출
     */
    public String extractConsumerGroupFromDltTopic(String dltTopic) {
        return topicUtils.extractConsumerGroup(dltTopic);
    }

    /**
     * 모든 카프카 토픽 목록 조회
     */
    private List<String> getAllTopics() {
        try {
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> topicNames = topicsResult.names().get();
            return new ArrayList<>(topicNames);
        } catch (Exception e) {
            log.error("토픽 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("토픽 목록 조회 실패", e);
        }
    }

    /**
     * 특정 토픽의 메시지 수 조회
     */
    public long getMessageCount(String topic) {
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer()) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .collect(Collectors.toList());

            if (partitions.isEmpty()) {
                return 0;
            }

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);

            return endOffsets.entrySet().stream()
                    .mapToLong(entry -> {
                        TopicPartition partition = entry.getKey();
                        long endOffset = entry.getValue();
                        long beginningOffset = beginningOffsets.getOrDefault(partition, 0L);
                        return Math.max(0, endOffset - beginningOffset);
                    })
                    .sum();
        } catch (Exception e) {
            log.error("토픽 {} 메시지 수 조회 실패: {}", topic, e.getMessage(), e);
            return 0;
        }
    }
}
